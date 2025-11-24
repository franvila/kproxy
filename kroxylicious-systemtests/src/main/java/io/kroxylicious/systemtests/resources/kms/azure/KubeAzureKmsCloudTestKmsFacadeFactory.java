/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.systemtests.resources.kms.azure;

import io.kroxylicious.kms.provider.azure.kms.AbstractAzureKeyVaultKmsTestKmsFacadeFactory;

/**
 * Factory for {@link KubeAzureKmsCloudTestKmsFacade}s.
 */
public class KubeAzureKmsCloudTestKmsFacadeFactory extends AbstractAzureKeyVaultKmsTestKmsFacadeFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public KubeAzureKmsCloudTestKmsFacade build() {
        return new KubeAzureKmsCloudTestKmsFacade();
    }
}
