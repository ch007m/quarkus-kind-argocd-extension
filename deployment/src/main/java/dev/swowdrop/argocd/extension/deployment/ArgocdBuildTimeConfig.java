package dev.swowdrop.argocd.extension.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.OptionalInt;

@ConfigMapping(prefix = "quarkus.argocd")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ArgocdBuildTimeConfig {
        /**
         * Configuration for the development services.
         */
        DevService devservices();

        interface DevService {
            /**
             * Whether devservice is enabled.
             */
            @WithDefault("false")
            boolean enabled();

            /**
             * If logs should be shown from the Argocd container.
             */
            @WithDefault("false")
            boolean showLogs();

            /**
             * The exposed HTTP port for the Argocd container.
             * If not specified, it will pick a random port
             */
            OptionalInt httpPort();
        }
}