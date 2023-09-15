/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.resources;

import java.time.Duration;

import io.strimzi.api.kafka.model.Kafka;

import io.kroxylicious.Constants;

public class ResourceOperation {
    public static long getTimeoutForResourceReadiness() {
        return getTimeoutForResourceReadiness("default");
    }

    public static long getTimeoutForResourceReadiness(String kind) {
        long timeout;

        switch (kind) {
            case Kafka.RESOURCE_KIND:
                timeout = Duration.ofMinutes(14).toMillis();
                break;
            case Constants.DEPLOYMENT:
                timeout = Duration.ofMinutes(8).toMillis();
                break;
            default:
                timeout = Duration.ofMinutes(3).toMillis();
        }

        return timeout;
    }

    /**
     * timeoutForPodsOperation returns a reasonable timeout in milliseconds for a number of Pods in a quorum to roll on update,
     *  scale up or create
     */
    public static long timeoutForPodsOperation(int numberOfPods) {
        return Duration.ofMinutes(5).toMillis() * Math.max(1, numberOfPods);
    }

    public static long getTimeoutForResourceDeletion() {
        return getTimeoutForResourceDeletion("default");
    }

    public static long getTimeoutForResourceDeletion(String kind) {
        long timeout;

        switch (kind) {
            case Kafka.RESOURCE_KIND:
            case Constants.POD_KIND:
                timeout = Duration.ofMinutes(5).toMillis();
                break;
            default:
                timeout = Duration.ofMinutes(3).toMillis();
        }

        return timeout;
    }
}