package dev.swowdrop.argocd.extension.deployment;

import com.dajudge.kindcontainer.client.KubeConfigUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesDevServiceInfoBuildItem;

import io.quarkus.devservices.common.ContainerShutdownCloseable;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Optional;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;

class ArgocdExtensionProcessor {
    private static final Logger LOG = Logger.getLogger(ArgocdExtensionProcessor.class);

    static volatile DevServicesResultBuildItem.RunningDevService devService;

    @BuildStep
    public void createHelloWorld(
        ArgocdBuildTimeConfig config,
        BuildProducer<ArgocdDevServiceInfoBuildItem> argocdDevServiceInfo) {

        var argocd = new ArgocdContainer(config.devservices());
        argocd.start();

        argocdDevServiceInfo.produce(new ArgocdDevServiceInfoBuildItem(
            argocd.getContainerName(),
            argocd.getHttpUrl(),
            argocd,
            argocd.getContainerId()
        ));
    }

    @BuildStep
    public DevServicesResultBuildItem deployArgocd(
        ArgocdBuildTimeConfig config,
        CuratedApplicationShutdownBuildItem closeBuildItem,
        ArgocdDevServiceInfoBuildItem argocdDevServiceInfo,
        KubernetesDevServiceInfoBuildItem kubeServiceInfo) {

        if (devService != null) {
            // only produce DevServicesResultBuildItem when the dev service first starts.
            return null;
        }

        if (!config.devservices().enabled()) {
            // Argocd Dev Service not enabled
            return null;
        }

        LOG.info(">>> Cluster container name : " + kubeServiceInfo.getName());
        kubeServiceInfo.getConfig().getClusters().stream().forEach(c -> {
            LOG.infof(">>> Cluster name: %s", c.getName());
            LOG.infof(">>> API URL: %s", c.getCluster().getServer());
        });
        kubeServiceInfo.getConfig().getUsers().stream().forEach(u -> LOG.infof(">>> User key: %s", u.getUser().getClientKeyData()));
        kubeServiceInfo.getConfig().getContexts().stream().forEach(ctx -> LOG.infof(">>> Context : %s", ctx.getContext().getUser()));

        KubernetesClient client = new KubernetesClientBuilder()
            .withConfig(kubeServiceInfo.getExtKubeConfig())
            .build();

        client.resources(Pod.class).inNamespace("default").list().getItems().forEach(pod -> {
            LOG.infof("Name: %s, status: %s", pod.getMetadata().getName(), pod.getStatus().getConditions().get(0).getStatus());
        });


/*        Config kubeConfig = kubeDevServiceClient.get().getConfig();
        kubeConfig.getContexts().stream().forEach(ctx -> LOG.info(">>> Kube ctx: " + ctx));
        LOG.infof(">>> Master URL is: %s", kubeConfig.getMasterUrl());*/

        // TODO: the code hereafter is not needed anymore as we can launch the kind container
        String httpUrl = argocdDevServiceInfo.httpUrl();
        LOG.infof("Argocd HTTP URL: %s", httpUrl);
        Map<String, String> configOverrides = Map.of("quarkus.argocd.devservices.http-url", httpUrl);

        ContainerShutdownCloseable closeable = new ContainerShutdownCloseable(argocdDevServiceInfo.container(), ArgocdProcessor.FEATURE);
        closeBuildItem.addCloseTask(closeable::close, true);
        devService = new DevServicesResultBuildItem.RunningDevService(ArgocdProcessor.FEATURE, argocdDevServiceInfo.containerId(), closeable, configOverrides);

        return devService.toBuildItem();
    }
}
