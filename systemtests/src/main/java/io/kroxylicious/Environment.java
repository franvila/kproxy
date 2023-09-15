/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Environment {

    private static final Map<String, String> VALUES = new HashMap<>();

    /**
     * Env. variables names
     */
    public static final String KAFKA_VERSION_ENV = "KAFKA_VERSION";

    /**
     * Env. variables defaults
     */
    public static final String KAFKA_VERSION_DEFAULT = "3.5.0";

    /**
     * Env. variables assignment
     */
    public static final String KAFKA_VERSION = getOrDefault(KAFKA_VERSION_ENV, KAFKA_VERSION_DEFAULT);

    private static String getOrDefault(String varName, String defaultValue) {
        return getOrDefault(varName, String::toString, defaultValue);
    }

    private static <T> T getOrDefault(String var, Function<String, T> converter, T defaultValue) {
        T returnValue = System.getenv(var) != null ? converter.apply(System.getenv(var)) : defaultValue;

        VALUES.put(var, String.valueOf(returnValue));
        return returnValue;
    }
}
