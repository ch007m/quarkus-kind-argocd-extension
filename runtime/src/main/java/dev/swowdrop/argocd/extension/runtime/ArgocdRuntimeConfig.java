package dev.swowdrop.argocd.extension.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

import java.net.URI;
import java.util.Optional;

@ConfigMapping(prefix = "quarkus.argocd")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ArgocdRuntimeConfig {
    /**
     * Configuration for the development services.
     */
    DevService devservices();

    interface DevService {
        /**
         * The HTTP URL of the dev services. Generated once the service is up and running.
         */
        Optional<URI> httpUrl();
    }
}