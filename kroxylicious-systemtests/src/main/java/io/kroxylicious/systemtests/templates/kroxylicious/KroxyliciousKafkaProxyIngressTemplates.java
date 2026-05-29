/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.systemtests.templates.kroxylicious;

import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyIngressBuilder;
import io.kroxylicious.systemtests.Constants;

import static io.kroxylicious.kubernetes.api.common.Protocol.TCP;

public class KroxyliciousKafkaProxyIngressTemplates {

    private KroxyliciousKafkaProxyIngressTemplates() {
    }

    /**
     * Kafka proxy ingress cluster IP CR.
     *
     * @return the Kafka proxy ingress builder
     */
    public static KafkaProxyIngressBuilder kafkaProxyIngressClusterIpCR() {
        // @formatter:off
        return new KafkaProxyIngressBuilder()
                .withNewMetadata()
                    .withName(Constants.KROXYLICIOUS_INGRESS_CLUSTER_IP)
                .endMetadata()
                .withNewSpec()
                    .withNewClusterIP()
                        .withProtocol(TCP)
                    .endClusterIP()
                    .withNewProxyRef()
                        .withName(Constants.KROXYLICIOUS_PROXY_SIMPLE_NAME)
                    .endProxyRef()
                .endSpec();
        // @formatter:on
    }

    public static KafkaProxyIngressBuilder kafkaProxyIngressLoadBalancerCR(String dnsName) {
        // @formatter:off
        return new KafkaProxyIngressBuilder()
                .withNewMetadata()
                    .withName(Constants.KROXYLICIOUS_INGRESS_LOAD_BALANCER)
                .endMetadata()
                .withNewSpec()
                    .withNewLoadBalancer()
                        .withBootstrapAddress("$(virtualClusterName)." + dnsName)
                        .withAdvertisedBrokerAddressPattern("broker$(nodeId).$(virtualClusterName)." + dnsName)
                    .endLoadBalancer()
                    .withNewProxyRef()
                        .withName(Constants.KROXYLICIOUS_PROXY_SIMPLE_NAME)
                    .endProxyRef()
                .endSpec();
        // @formatter:on
    }
}
