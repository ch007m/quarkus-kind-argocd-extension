package dev.swowdrop.argocd.extension.deployment;

import com.dajudge.kindcontainer.client.config.KubeConfig;
import io.quarkus.builder.item.SimpleBuildItem;

public final class ArgocdDevServiceInfoBuildItem extends SimpleBuildItem {
    private final String name;
    private final KubeConfig kubeConfig;
    private final String containerId;

    public ArgocdDevServiceInfoBuildItem(String name, KubeConfig kubeConfig, String containerId) {
        this.name = name;
        this.kubeConfig = kubeConfig;
        this.containerId = containerId;
    }

    public String name() {
        return name;
    }
    public KubeConfig kubeConfig() {return kubeConfig;}
    public String containerId() { return containerId; }
}
