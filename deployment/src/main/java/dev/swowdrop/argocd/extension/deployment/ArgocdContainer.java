package dev.swowdrop.argocd.extension.deployment;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.quarkus.devservices.common.JBossLoggingConsumer;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class ArgocdContainer extends GenericContainer<ArgocdContainer> {
    private static final Logger LOG = Logger.getLogger(ArgocdContainer.class);
    private static final int SERVICE_PORT = 8000;
    private static final int HTTP_PORT = 8000;
    private ArgocdBuildTimeConfig.DevService devServiceConfig;

    ArgocdContainer(ArgocdBuildTimeConfig.DevService devServiceConfig) {
        super("crccheck/hello-world");
        this.devServiceConfig = devServiceConfig;
        withExposedPorts(SERVICE_PORT);
        withReuse(true);
        waitingFor(Wait.forLogMessage(".*" + "httpd" + ".*", 1));
        withStartupAttempts(2);

        devServiceConfig.httpPort().ifPresent(port -> addFixedExposedPort(port, HTTP_PORT));

        if (devServiceConfig.showLogs()) {
            withLogConsumer(new JBossLoggingConsumer(LOG));
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        // TODO Create some additional stuffs here such as admin user, password, etc
    }

    public String getHttpUrl() {
        return "http://" + getHost() + ":" + getHttpPort();
    }

    public int getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }
}
