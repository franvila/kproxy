/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.systemtests.resources.kms.azure;

import java.net.URI;

import io.kroxylicious.kms.provider.azure.config.AzureKeyVaultConfig;
import io.kroxylicious.kms.provider.azure.config.auth.ManagedIdentityCredentialsConfig;
import io.kroxylicious.kms.provider.azure.config.auth.Oauth2ClientCredentialsConfig;
import io.kroxylicious.kms.provider.azure.kms.AbstractAzureKeyVaultKmsTestKmsFacade;
import io.kroxylicious.kms.service.TestKekManager;
import io.kroxylicious.kms.service.TestKmsFacadeException;
import io.kroxylicious.proxy.config.secret.InlinePassword;
import io.kroxylicious.systemtests.Environment;
import io.kroxylicious.systemtests.installation.kms.azure.AzureKmsCloud;

/**
 * KMS Facade for Azure Kms Cloud.
 */
public class KubeAzureKmsCloudTestKmsFacade extends AbstractAzureKeyVaultKmsTestKmsFacade {
    private final AzureKmsCloud azureKmsCloud;

    /**
     * Instantiates a new Kube Azure Kms Cloud test kms facade.
     *
     */
    public KubeAzureKmsCloudTestKmsFacade() {
        this.azureKmsCloud = new AzureKmsCloud();
    }

    @Override
    public boolean isAvailable() {
        return azureKmsCloud.isAvailable();
    }

    @Override
    public TestKekManager getTestKekManager() {
        return new AzureKmsTestKekManager(azureKmsCloud.getEndpointAuthority(), azureKmsCloud.getEndpointAuthority(), azureKmsCloud.getDefaultVaultBaseUrl().toString());
    }

    @Override
    public void startKms() {
        azureKmsCloud.deploy();
    }

    @Override
    public void stopKms() {
        azureKmsCloud.delete();
    }

    @Override
    public final AzureKeyVaultConfig getKmsServiceConfig() {
        try {
            // String password = DeploymentUtils.getSecretValue(azureKmsCloud.getDefaultNamespace(), Constants.KEYSTORE_SECRET_NAME, "password");
            // String keyVaultHost = azureKmsCloud.getDefaultNamespace() + ".svc.cluster.local";

            // TrustStore vaultTrust = new TrustStore("${secret:" + Constants.KEYSTORE_SECRET_NAME + ":" + Constants.KEYSTORE_FILE_NAME + "}",
            // new InlinePassword(password), "JKS");
            // Tls vaultTls = new Tls(null, vaultTrust, null, null);
            // TrustStore entraTrust = new TrustStore("${secret:" + Constants.TRUSTSTORE_SECRET_NAME + ":" + Constants.TRUSTSTORE_FILE_NAME + "}",
            // new InlinePassword(password), "JKS");
            // Tls entraTls = new Tls(null, entraTrust, null, null);
            ManagedIdentityCredentialsConfig managedIdentityCredentialsConfig = new ManagedIdentityCredentialsConfig("https://vault.azure.net/", null);
            return new AzureKeyVaultConfig(
                    new Oauth2ClientCredentialsConfig(azureKmsCloud.getOauthBaseUri(), Environment.AZURE_KMS_TENANT_ID,
                            new InlinePassword(Environment.AZURE_KMS_CLIENT_ID), new InlinePassword(Environment.AZURE_KMS_CLIENT_SECRET),
                            URI.create("https://vault.azure.net/.default"), null),
                    managedIdentityCredentialsConfig, AzureKmsCloud.KEY_VAULT_NAME, "vault.azure.net", null, null, null);
        }
        catch (Exception e) {
            throw new TestKmsFacadeException(e);
        }
    }
}
