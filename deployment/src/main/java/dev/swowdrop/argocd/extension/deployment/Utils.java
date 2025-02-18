package dev.swowdrop.argocd.extension.deployment;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class Utils {
    private static final Logger LOG = Logger.getLogger(Utils.class);
    private static ArgocdBuildTimeConfig.DevService config;
    private static KubernetesClient client;

    protected static InputStream fetchResourcesFromURL(String version) {
        InputStream resourceAsStream = null;
        try {
            if (version == "latest") {
                resourceAsStream = new URL("https://raw.githubusercontent.com/argoproj/argo-cd/refs/heads/master/manifests/install.yaml").openStream();
            } else {
                resourceAsStream = new URL("https://raw.githubusercontent.com/argoproj/argo-cd/refs/tags/" + version + "/manifests/install.yaml").openStream();
            }
        } catch (Exception e) {
            LOG.error("The resources cannot be fetched from the argocd repository URL !");
            LOG.error(e);
        }
        return resourceAsStream;
    }

    protected static void waitTillPodReady(String name) {
        client.resources(Pod.class)
            .inNamespace(config.controllerNamespace())
            .withName(name)
            .waitUntilReady(config.timeOut(), TimeUnit.SECONDS);
        LOG.infof("Pod: %s ready in %s", name, config.controllerNamespace());
    }

    protected static void waitTillPodByLabelReady(String key, String value) {
        client.resources(Pod.class)
            .inNamespace(config.controllerNamespace())
            .withLabel(key, value)
            .waitUntilReady(config.timeOut(), TimeUnit.SECONDS);
        LOG.infof("Pod: %s ready in %s", value, config.controllerNamespace());
    }

    public static void setKubernetesClient(KubernetesClient client) {
        Utils.client = client;
    }

    public static void setConfig(ArgocdBuildTimeConfig.DevService devservices) {
        Utils.config = devservices;
    }
}
