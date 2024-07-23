/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.service;

import java.util.Objects;

/**
 * Exposes the ability to manage the KEKs on a KMS implementation.
 */
public interface TestKekManager {
    /**
     * Creates a KEK in the KMS with given alias.
     *
     * @param alias kek alias
     * @throws AlreadyExistsException alias already exists
     */
    default void generateKek(String alias) {
        Objects.requireNonNull(alias);

        if (exists(alias)) {
            throw new AlreadyExistsException(alias);
        }
        else {
            create(alias);
        }
    }

    /**
     * Removes a KEK from the KMS with given alias.
     *
     * @param alias kek alias
     * @throws UnknownAliasException alias already exists
     */
    default void deleteKek(String alias) {
        if (!exists(alias)) {
            throw new UnknownAliasException(alias);
        }
        else {
            delete(alias);
        }
    }

    /**
     * Rotates the kek with the given alias
     *
     * @param alias kek alias
     * @throws UnknownAliasException a KEK with the given alias is not found
     */
    default void rotateKek(String alias) {
        Objects.requireNonNull(alias);

        if (!exists(alias)) {
            throw new UnknownAliasException(alias);
        }
        else {
            rotate(alias);
        }
    }

    /**
     * Tests whether kek with given alias exists.
     *
     * @param alias kek alias
     * @return true if the alias exist, false otherwise.
     */
    default boolean exists(String alias) {
        try {
            read(alias);
            return true;
        }
        catch (UnknownAliasException uae) {
            return false;
        }
    }

    void create(String alias);
    Object read(String alias);
    void rotate(String alias);
    void delete(String alias);

    class AlreadyExistsException extends KmsException {
        public AlreadyExistsException(String alias) {
            super(alias);
        }
    }
}
