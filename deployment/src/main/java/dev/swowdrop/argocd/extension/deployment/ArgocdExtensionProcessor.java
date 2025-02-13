package dev.swowdrop.argocd.extension.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

class ArgocdExtensionProcessor {
    private static final Logger LOG = Logger.getLogger(ArgocdExtensionProcessor.class);
    private static final String FEATURE = "argocd-extension";
    private static final int SERVICE_PORT = 8000;

    @BuildStep
    public DevServicesResultBuildItem feature(ApplicationInfoBuildItem info) {
        var name = info.getName();
        var version = info.getVersion();

        DockerImageName dockerImageName = DockerImageName.parse("crccheck/hello-world");
        GenericContainer container = new GenericContainer<>(dockerImageName)
            .withExposedPorts(SERVICE_PORT)
            .waitingFor(Wait.forLogMessage(".*" + "httpd" + ".*", 1))
            .withReuse(true);
        container.start();

        String newUrl = "http://" + container.getHost() + ":" + container.getMappedPort(SERVICE_PORT);
        Map<String, String> configOverrides = Map.of("hello-world.base-url", newUrl);

        LOG.info("The Hello World service is available at the url: " + newUrl);

        return new DevServicesResultBuildItem.RunningDevService(FEATURE, container.getContainerId(),
            container::close, configOverrides)
            .toBuildItem();
    }
}
