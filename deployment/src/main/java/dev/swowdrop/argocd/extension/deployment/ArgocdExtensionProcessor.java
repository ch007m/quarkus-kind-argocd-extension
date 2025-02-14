package dev.swowdrop.argocd.extension.deployment;

import io.fabric8.kubernetes.client.Config;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;

import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.jgit.deployment.GiteaDevServiceInfoBuildItem;
import io.quarkus.jgit.deployment.GiteaDevServiceRequestBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesResourcesBuildItem;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class ArgocdExtensionProcessor {
    private static final Logger LOG = Logger.getLogger(ArgocdExtensionProcessor.class);

    static volatile DevServicesResultBuildItem.RunningDevService devService;

    @BuildStep
    void requestGitea(ArgocdBuildTimeConfig config, ApplicationInfoBuildItem applicationInfo,
        BuildProducer<GiteaDevServiceRequestBuildItem> giteaDevServiceRequest) {
            if (config.devservices().enabled()) {
                giteaDevServiceRequest.produce(
                    new GiteaDevServiceRequestBuildItem("gitea", List.of("dev"), List.of("dev/" + applicationInfo.getName())));
            }
        }

    @BuildStep
    public DevServicesResultBuildItem feature(
        ArgocdBuildTimeConfig config,
        KubernetesClientBuildItem kubeClient,
        CuratedApplicationShutdownBuildItem closeBuildItem,
        Optional<GiteaDevServiceInfoBuildItem> giteaServiceInfo) {

        if (devService != null) {
            // only produce DevServicesResultBuildItem when the dev service first starts.
            return null;
        }

        if (!config.devservices().enabled()) {
            // Argocd Dev Service not enabled
            return null;
        }
        var argocd = new ArgocdContainer(config.devservices());
        argocd.start();

        Map<String, String> configOverrides = new HashMap<>();
        giteaServiceInfo.ifPresent(gitea -> {
            configOverrides.put("quarkus.argocd.git.url", "http://" + gitea.host() + ":" + gitea.httpPort() + "/");
            configOverrides.put("quarkus.argocd.git.username", gitea.adminUsername());
            configOverrides.put("quarkus.argocd.git.password", gitea.adminPassword());
        });

        Config kubeConfig = kubeClient.getConfig();
        kubeConfig.getContexts().stream().forEach(ctx -> LOG.info("Kube ctx: " + ctx));

        String httpUrl = argocd.getHttpUrl();
        LOG.infof("Argocd HTTP URL: %s", httpUrl);
        Map<String, String> configOverrides = Map.of("quarkus.argocd.devservices.http-url", httpUrl);

        ContainerShutdownCloseable closeable = new ContainerShutdownCloseable(argocd, ArgocdProcessor.FEATURE);
        closeBuildItem.addCloseTask(closeable::close, true);
        devService = new DevServicesResultBuildItem.RunningDevService(ArgocdProcessor.FEATURE, argocd.getContainerId(), closeable, configOverrides);

        return devService.toBuildItem();
    }
}
