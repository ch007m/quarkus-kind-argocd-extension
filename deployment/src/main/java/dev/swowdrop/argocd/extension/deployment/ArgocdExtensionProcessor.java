package dev.swowdrop.argocd.extension.deployment;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesDevServiceInfoBuildItem;

import io.quarkus.devservices.common.ContainerShutdownCloseable;
import org.jboss.logging.Logger;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.swowdrop.argocd.extension.deployment.Utils.waitTillPodByLabelReady;
import static dev.swowdrop.argocd.extension.deployment.Utils.waitTillPodReady;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArgocdExtensionProcessor {
    private static final Logger LOG = Logger.getLogger(ArgocdExtensionProcessor.class);

    private static final String ARGOCD_DEX_SERVER_NAME = "argocd-dex-server";
    private static final String ARGOCD_NOTIFICATION_CONTROLLER_NAME = "argocd-notifications-controller";
    private static final String ARGOCD_APP_CONTROLLER_NAME = "argocd-application-controller";
    private static final String ARGOCD_APPLICATIONSET_CONTROLLER_NAME = "argocd-applicationset-controller";
    private static final String ARGOCD_SERVER_NAME = "argocd-server";
    private static final String ARGOCD_REDIS_NAME = "argocd-redis";
    private static final String ARGOCD_REPO_SERVER_NAME = "argocd-repo-server";

    private static final String ARGOCD_INITIAL_ADMIN_SECRET_NAME = "argocd-initial-admin-secret";

    static volatile DevServicesResultBuildItem.RunningDevService devService;

    @BuildStep
    public DevServicesResultBuildItem deployArgocd(
        ArgocdBuildTimeConfig config,
        BuildProducer<ArgocdDevServiceInfoBuildItem> argocdDevServiceInfo,
        KubernetesDevServiceInfoBuildItem kubeServiceInfo) {

        if (devService != null) {
            // only produce DevServicesResultBuildItem when the dev service first starts.
            return null;
        }

        if (!config.devservices().enabled()) {
            // Argocd Dev Service not enabled
            return null;
        }
        
        if (config.devservices().debugEnabled()) {
            LOG.info(">>> Cluster container name : " + kubeServiceInfo.getName());
            kubeServiceInfo.getConfig().getClusters().stream().forEach(c -> {
                LOG.debugf(">>> Cluster name: %s", c.getName());
                LOG.debugf(">>> API URL: %s", c.getCluster().getServer());
            });
            kubeServiceInfo.getConfig().getUsers().stream().forEach(u -> LOG.debugf(">>> User key: %s", u.getUser().getClientKeyData()));
            kubeServiceInfo.getConfig().getContexts().stream().forEach(ctx -> LOG.debugf(">>> Context : %s", ctx.getContext().getUser()));
        }
        
        var ARGOCD_CONTROLLER_NAMESPACE = config.devservices().controllerNamespace();

        KubernetesClient client = new KubernetesClientBuilder()
            .withConfig(Config.fromKubeconfig(kubeServiceInfo.getExtKubeConfig()))
            .build();

        // Pass the configuration parameters to the utility class
        Utils.setConfig(config.devservices());
        Utils.setKubernetesClient(client);

        // Install the Argocd resources from the YAML manifest file
        List<HasMetadata> items = client.load(Utils.fetchResourcesFromURL(config.devservices().version())).items();

        LOG.infof("Creating the argocd controller namespace: %s", ARGOCD_CONTROLLER_NAMESPACE);
        // @formatter:off
        client.namespaces()
            .resource(new NamespaceBuilder()
                .withNewMetadata()
                  .withName(ARGOCD_CONTROLLER_NAMESPACE)
                .endMetadata()
                .build())
            .create();
        // @formatter:on

        // Deploy the different resources: Service, CRD, Deployment, ConfigMap
        // EXCEPT: Notification & Dex server as non needed
        List<HasMetadata> filteredItems = items.stream()
            .filter(r -> !(r instanceof Deployment &&
                (ARGOCD_DEX_SERVER_NAME.equals(r.getMetadata().getName()) || ARGOCD_NOTIFICATION_CONTROLLER_NAME.equals(r.getMetadata().getName()))))
            .collect(Collectors.toList());

        LOG.info("Deploying the argocd resources ...");
        for (HasMetadata item : filteredItems) {
            var res = client.resource(item).inNamespace(ARGOCD_CONTROLLER_NAMESPACE);
            res.create();
            assertNotNull(res);
        };

        // Waiting till the pods are ready/running ...
        waitTillPodByLabelReady("app.kubernetes.io/name",ARGOCD_REDIS_NAME);
        waitTillPodByLabelReady("app.kubernetes.io/name",ARGOCD_REPO_SERVER_NAME);
        waitTillPodByLabelReady("app.kubernetes.io/name",ARGOCD_SERVER_NAME);
        waitTillPodByLabelReady("app.kubernetes.io/name",ARGOCD_APPLICATIONSET_CONTROLLER_NAME);
        waitTillPodReady(ARGOCD_APP_CONTROLLER_NAME + "-0");

        if (config.devservices().debugEnabled()) {
            // List the pods running under the Argocd controller namespace
            client.resources(Pod.class)
                .inNamespace(ARGOCD_CONTROLLER_NAMESPACE)
                .list().getItems().stream().forEach(p -> {
                    LOG.infof("Pod : %, status: %s", p.getMetadata().getName(), p.getStatus().getConditions().get(0).getStatus());
                });
        }
        
        // Get the argocd admin secret
        var argocd_admin_password = client.resources(Secret.class)
            .inNamespace(ARGOCD_CONTROLLER_NAMESPACE).withName(ARGOCD_INITIAL_ADMIN_SECRET_NAME)
            .get().getData().get("password");
        LOG.infof("Argocd admin password : %s", Base64.getDecoder().decode(argocd_admin_password).toString());

        // TODO: To be reviewed in order to pass argocd parameters for the service consuming the extension
        Map<String, String> configOverrides = Map.of(
            "quarkus.argocd.devservices.controller-namespace", ARGOCD_CONTROLLER_NAMESPACE,
            "quarkus.argocd.devservices.admin-password", Base64.getDecoder().decode(argocd_admin_password).toString(),
            "quarkus.argocd.devservices.kube-config",kubeServiceInfo.getExtKubeConfig());

        argocdDevServiceInfo.produce(new ArgocdDevServiceInfoBuildItem(
            kubeServiceInfo.getName(),
            kubeServiceInfo.getConfig(),
            kubeServiceInfo.getContainer().getContainerId()));

        return new DevServicesResultBuildItem.RunningDevService(
            ArgocdProcessor.FEATURE,
            kubeServiceInfo.getContainer().getContainerId(),
            new ContainerShutdownCloseable(kubeServiceInfo.getContainer(), ArgocdProcessor.FEATURE),
            configOverrides)
        .toBuildItem();
    }
}
