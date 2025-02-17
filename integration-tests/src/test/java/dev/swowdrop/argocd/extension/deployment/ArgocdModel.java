package dev.swowdrop.argocd.extension.deployment;

import io.quarkiverse.argocd.v1alpha1.AppProject;
import io.quarkiverse.argocd.v1alpha1.Application;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ArgocdModel {
    public String username;
    public String password;
    public Application application;
    public AppProject appProject;
}
