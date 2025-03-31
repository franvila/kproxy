/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.systemtests.templates.kroxylicious;

import io.kroxylicious.kubernetes.api.common.KafkaServiceRefBuilder;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaClusterBuilder;

public class KroxyliciousVirtualKafkaClusterTemplates {

    private KroxyliciousVirtualKafkaClusterTemplates() {
    }

    public static VirtualKafkaClusterBuilder defaultVirtualKafkaClusterDeployment(String namespaceName, String clusterName, String proxy, String clusterRef,
                                                                                  String ingressName) {
        // @formatter:off
        return new VirtualKafkaClusterBuilder()
                .withNewMetadata()
                    .withName(clusterName)
                    .withNamespace(namespaceName)
                .endMetadata()
                .withNewSpec()
                    .withTargetKafkaServiceRef(new KafkaServiceRefBuilder()
                            .withName(clusterRef)
                            .build())
                    .withNewProxyRef()
                        .withName(proxy)
                    .endProxyRef()
                    .addNewIngressRef()
                        .withName(ingressName)
                    .endIngressRef()
                .endSpec();
        // @formatter:on
    }
}