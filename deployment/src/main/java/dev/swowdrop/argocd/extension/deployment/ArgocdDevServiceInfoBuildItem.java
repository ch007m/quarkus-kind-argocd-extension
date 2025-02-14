package dev.swowdrop.argocd.extension.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import org.testcontainers.containers.GenericContainer;

public final class ArgocdDevServiceInfoBuildItem extends SimpleBuildItem {
    private final String name;
    private final String httpUrl;
    private final GenericContainer<ArgocdContainer> container;
    private final String containerId;

    public ArgocdDevServiceInfoBuildItem(String name, String httpUrl, GenericContainer<ArgocdContainer> container, String containerId) {
        this.name = name;
        this.httpUrl = httpUrl;
        this.container = container;
        this.containerId = containerId;
    }

    public String name() {
        return name;
    }
    public String httpUrl() {
        return httpUrl;
    }
    public GenericContainer<ArgocdContainer> container() {return container;}
    public String containerId() {return containerId;}
}
