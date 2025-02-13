package dev.swowdrop.argocd.extension.deployment;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;

import io.quarkus.devservices.common.ContainerShutdownCloseable;
import org.jboss.logging.Logger;

import java.util.Map;

class ArgocdExtensionProcessor {
    private static final Logger LOG = Logger.getLogger(ArgocdExtensionProcessor.class);

    static volatile DevServicesResultBuildItem.RunningDevService devService;

    @BuildStep
    public DevServicesResultBuildItem feature(
        ArgocdBuildTimeConfig config,
        CuratedApplicationShutdownBuildItem closeBuildItem) {

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

        String httpUrl = argocd.getHttpUrl();
        LOG.infof("Argocd HTTP URL: %s", httpUrl);
        Map<String, String> configOverrides = Map.of("quarkus.argocd.devservices.http-url", httpUrl);

        ContainerShutdownCloseable closeable = new ContainerShutdownCloseable(argocd, ArgocdProcessor.FEATURE);
        closeBuildItem.addCloseTask(closeable::close, true);
        devService = new DevServicesResultBuildItem.RunningDevService(ArgocdProcessor.FEATURE, argocd.getContainerId(), closeable, configOverrides);

        return devService.toBuildItem();
    }
}
