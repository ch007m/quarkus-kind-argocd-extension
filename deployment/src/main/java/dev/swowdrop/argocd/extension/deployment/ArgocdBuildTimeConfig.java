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
         * Whether debugging level is enabled.
         */
        @WithDefault("false")
        boolean debugEnabled();

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
         * The version of Argocd to be installed from the GitHub repository
         * If not specified, it will use the resources published on master branch
         * The version to be used should be specified using the tagged release: v2.14.3, etc
         */
        @WithDefault("latest")
        String version();

        /**
         * The Argocd controllers namespace where: Application, ApplicationSet, etc. are deployed and running
         * The default namespace is: argocd
         */
        @WithDefault("argocd")
        String controllerNamespace();

        /**
         * Time to wait till a resource is ready: pod, etc
         * The default value is: 180 seconds
         */
        @WithDefault("180")
        long timeOut();
    }
}