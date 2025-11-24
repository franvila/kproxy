/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.systemtests.installation.kms.azure;

import java.net.URI;

import io.kroxylicious.systemtests.Environment;

/**
 * The type Aws kms cloud.
 */
public class AzureKmsCloud implements AzureKmsClient {
    public static final String KEY_VAULT_NAME = "kroxylicious-st-vault";

    /**
     * Instantiates a new Aws.
     *
     */
    public AzureKmsCloud() {
        // nothing to do
    }

    @Override
    public boolean isAvailable() {
        return Environment.USE_CLOUD_KMS.equalsIgnoreCase("true");
    }

    @Override
    public void deploy() {
        // nothing to deploy
    }

    @Override
    public URI getDefaultVaultBaseUrl() {
        return URI.create("https://" + this.getEndpointAuthority());
    }

    @Override
    public String getEndpointAuthority() {
        return KEY_VAULT_NAME + ".vault.azure.net";
    }

    public URI getOauthBaseUri() {
        return URI.create("https://login.microsoftonline.com");
    }

    @Override
    public void delete() {
        // nothing to delete
    }
}
