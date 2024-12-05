/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.systemtests.installation.kroxylicious;

import io.kroxylicious.systemtests.resources.operator.KroxyliciousOperatorBundleInstaller;

public class KroxyliciousOperator {
    private final String deploymentNamespace;

    public KroxyliciousOperator(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
    }

    public void deployBundle() {
        // SetUpKroxyliciousOperator kroxyliciousOperator = SetUpKroxyliciousOperator.getInstance();
        // kroxyliciousOperator.defaultInstallation().withNamespace(deploymentNamespace).createBundleInstallation().runBundleInstallation();
        KroxyliciousOperatorBundleInstaller.getInstance().getDefaultBuilder().withNamespace(deploymentNamespace).createBundleInstallation().install();
    }

    public void delete() {
        KroxyliciousOperatorBundleInstaller.getInstance().uninstall();
    }
}
