/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import java.util.UUID;
import java.util.concurrent.CompletionException;

import io.kroxylicious.kms.provider.kroxylicious.inmemory.IntegrationTestingKmsService.Config;
import io.kroxylicious.kms.service.TestKekManager;
import io.kroxylicious.kms.service.TestKmsFacade;

public class InMemoryTestKmsFacade implements TestKmsFacade<Config, UUID, InMemoryEdek> {

    private final UUID kmsId = UUID.randomUUID();
    private InMemoryKms kms;

    @Override
    public void start() {
        kms = IntegrationTestingKmsService.newInstance().buildKms(new Config(kmsId.toString()));
    }

    @Override
    public void stop() {
        IntegrationTestingKmsService.delete(kmsId.toString());
    }

    @Override
    public TestKekManager getTestKekManager() {
        return new InMemoryTestKekManager();
    }

    @Override
    public Class<IntegrationTestingKmsService> getKmsServiceClass() {
        return IntegrationTestingKmsService.class;
    }

    @Override
    public InMemoryKms getKms() {
        return kms;
    }

    @Override
    public Config getKmsServiceConfig() {
        return new Config(kmsId.toString());
    }

    private class InMemoryTestKekManager implements TestKekManager {

        @Override
        public void create(String alias) {
            var kekId = kms.generateKey();
            kms.createAlias(kekId, alias);
        }

        @Override
        public UUID read(String alias) {
            try {
                return kms.resolveAlias(alias).toCompletableFuture().join();
            }
            catch (CompletionException e) {
                throw unwrapRuntimeException(e);
            }
        }

        @Override
        public void rotate(String alias) {
            create(alias);
        }

        @Override
        public void delete(String alias) {
            var kekRef = read(alias);
            kms.deleteAlias(alias);
            kms.deleteKey(kekRef);
        }

        private RuntimeException unwrapRuntimeException(Exception e) {
            return e.getCause() instanceof RuntimeException re ? re : new RuntimeException(e.getCause());
        }
    }
}
