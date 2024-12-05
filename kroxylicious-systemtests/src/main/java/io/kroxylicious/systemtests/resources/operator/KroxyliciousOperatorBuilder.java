/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.systemtests.resources.operator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;

public class KroxyliciousOperatorBuilder {

    public ExtensionContext extensionContext;
    public String clusterOperatorName;
    public String namespaceInstallTo;
    public String namespaceToWatch;
    public List<String> bindingsNamespaces;
    public Duration operationTimeout;
    public Duration reconciliationInterval;
    public Map<String, String> extraLabels;
    public int replicas = 1;
    public List<RoleBinding> roleBindings;
    public List<Role> roles;
    public List<ClusterRole> clusterRoles;
    public List<ClusterRoleBinding> clusterRoleBindings;

    public KroxyliciousOperatorBuilder withExtensionContext(ExtensionContext extensionContext) {
        this.extensionContext = extensionContext;
        return self();
    }

    public KroxyliciousOperatorBuilder withClusterOperatorName(String clusterOperatorName) {
        this.clusterOperatorName = clusterOperatorName;
        return self();
    }

    public KroxyliciousOperatorBuilder withNamespace(String namespaceInstallTo) {
        this.namespaceInstallTo = namespaceInstallTo;
        return self();
    }

    public KroxyliciousOperatorBuilder withWatchingNamespaces(String namespaceToWatch) {
        this.namespaceToWatch = namespaceToWatch;
        return self();
    }

    public KroxyliciousOperatorBuilder addToTheWatchingNamespaces(String namespaceToWatch) {
        if (this.namespaceToWatch != null) {
            if (!this.namespaceToWatch.equals("*")) {
                this.namespaceToWatch += "," + namespaceToWatch;
            }
        }
        else {
            this.namespaceToWatch = namespaceToWatch;
        }
        return self();
    }

    public KroxyliciousOperatorBuilder withBindingsNamespaces(List<String> bindingsNamespaces) {
        this.bindingsNamespaces = bindingsNamespaces;
        return self();
    }

    public KroxyliciousOperatorBuilder addToTheBindingsNamespaces(String bindingsNamespace) {
        if (this.bindingsNamespaces != null) {
            this.bindingsNamespaces = new ArrayList<>(this.bindingsNamespaces);
        }
        else {
            this.bindingsNamespaces = new ArrayList<>(Collections.singletonList(bindingsNamespace));
        }
        return self();
    }

    public KroxyliciousOperatorBuilder withOperationTimeout(Duration operationTimeout) {
        this.operationTimeout = operationTimeout;
        return self();
    }

    public KroxyliciousOperatorBuilder withReconciliationInterval(Duration reconciliationInterval) {
        this.reconciliationInterval = reconciliationInterval;
        return self();
    }

    // currently supported only for Bundle installation
    public KroxyliciousOperatorBuilder withExtraLabels(Map<String, String> extraLabels) {
        this.extraLabels = extraLabels;
        return self();
    }

    public KroxyliciousOperatorBuilder withReplicas(int replicas) {
        this.replicas = replicas;
        return self();
    }

    public KroxyliciousOperatorBuilder withRoleBindings(final List<RoleBinding> roleBindings) {
        this.roleBindings = roleBindings;
        return self();
    }

    public KroxyliciousOperatorBuilder withRoles(final List<Role> roles) {
        this.roles = roles;
        return self();
    }

    public KroxyliciousOperatorBuilder withClusterRoles(final List<ClusterRole> clusterRoles) {
        this.clusterRoles = clusterRoles;
        return self();
    }

    public KroxyliciousOperatorBuilder withClusterRoleBindings(final List<ClusterRoleBinding> clusterRoleBindings) {
        this.clusterRoleBindings = clusterRoleBindings;
        return self();
    }

    private KroxyliciousOperatorBuilder self() {
        return this;
    }

    public KroxyliciousOperatorInstaller createBundleInstallation() {
        return new KroxyliciousOperatorBundleInstaller(this);
    }
}
