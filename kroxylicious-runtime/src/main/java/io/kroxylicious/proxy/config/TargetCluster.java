/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.config;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.kroxylicious.proxy.config.tls.Tls;
import io.kroxylicious.proxy.service.HostPort;

/**
 * Represents the target (upstream) kafka cluster.
 *
 * @param bootstrapServers A list of host/port pairs to use for establishing the initial connection to the target (upstream) Kafka cluster.
 * @param tls tls configuration if a secure connection is to be used.
 */
public record TargetCluster(@JsonProperty(value = "bootstrapServers", required = true) String bootstrapServers,
                            @JsonProperty(value = "tls") Optional<Tls> tls) {

    public TargetCluster {
        if (bootstrapServers == null) {
            throw new IllegalArgumentException("'bootstrapServers' is required in a target cluster.");
        }
    }

    /**
     * A list of host/port pairs to use for establishing the initial connection to the target (upstream) Kafka cluster.
     * This list should be in the form host1:port1,host2:port2,...
     *
     * @return comma separated list of bootstrap servers.
     */
    @Override
    public String bootstrapServers() {
        return bootstrapServers;
    }

    public List<HostPort> bootstrapServersList() {
        return Arrays.stream(bootstrapServers.split(",")).map(HostPort::parse).toList();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TargetCluster[");
        sb.append("bootstrapServers='").append(bootstrapServers).append('\'');
        sb.append(", tls=").append(tls.map(Tls::toString).orElse(null));
        sb.append(']');
        return sb.toString();
    }
}
