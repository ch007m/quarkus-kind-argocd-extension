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
    void requestKind(ArgocdBuildTimeConfig config,
                     BuildProducer<KubernetesClientBuildItem> kubeDevServiceRequest) {
        if (config.devservices().enabled()) {
            kubeDevServiceRequest.produce(new KubernetesClientBuildItem(null,null));
        }
    }

    @BuildStep
    public DevServicesResultBuildItem feature(
        ArgocdBuildTimeConfig config,
        CuratedApplicationShutdownBuildItem closeBuildItem,
        Optional<KubernetesClientBuildItem> kubeDevServiceRequest,
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

        giteaServiceInfo.ifPresent(gitea -> {
            LOG.info("Gitea host is available : " + gitea.host());
        });

        Config kubeConfig = kubeDevServiceRequest.get().getConfig();
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
