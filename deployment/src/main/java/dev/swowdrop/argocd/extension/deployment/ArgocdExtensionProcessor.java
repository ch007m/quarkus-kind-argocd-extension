package dev.swowdrop.argocd.extension.deployment;

import io.fabric8.kubernetes.client.Config;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;

import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Optional;

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
        Optional<KubernetesClientBuildItem> kubeDevServiceClient,
        ArgocdDevServiceInfoBuildItem argocdDevServiceInfo) {

        if (devService != null) {
            // only produce DevServicesResultBuildItem when the dev service first starts.
            return null;
        }

        if (!config.devservices().enabled()) {
            // Argocd Dev Service not enabled
            return null;
        }

        Config kubeConfig = kubeDevServiceClient.get().getConfig();
        kubeConfig.getContexts().stream().forEach(ctx -> LOG.info("Kube ctx: " + ctx));

        String httpUrl = argocdDevServiceInfo.httpUrl();
        LOG.infof("Argocd HTTP URL: %s", httpUrl);
        Map<String, String> configOverrides = Map.of("quarkus.argocd.devservices.http-url", httpUrl);

        ContainerShutdownCloseable closeable = new ContainerShutdownCloseable(argocdDevServiceInfo.container(), ArgocdProcessor.FEATURE);
        closeBuildItem.addCloseTask(closeable::close, true);
        devService = new DevServicesResultBuildItem.RunningDevService(ArgocdProcessor.FEATURE, argocdDevServiceInfo.containerId(), closeable, configOverrides);

        return devService.toBuildItem();
    }
}
