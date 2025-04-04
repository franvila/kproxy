/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kubernetes.operator.resolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.javaoperatorsdk.operator.api.reconciler.Context;

import io.kroxylicious.kubernetes.api.common.FilterRef;
import io.kroxylicious.kubernetes.api.common.FilterRefBuilder;
import io.kroxylicious.kubernetes.api.common.IngressRef;
import io.kroxylicious.kubernetes.api.common.IngressRefBuilder;
import io.kroxylicious.kubernetes.api.common.KafkaServiceRefBuilder;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxy;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyIngress;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyIngressBuilder;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaService;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaServiceBuilder;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaCluster;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaClusterBuilder;
import io.kroxylicious.kubernetes.filter.api.v1alpha1.KafkaProtocolFilter;
import io.kroxylicious.kubernetes.filter.api.v1alpha1.KafkaProtocolFilterBuilder;
import io.kroxylicious.kubernetes.operator.resolver.ResolutionResult.ClusterResolutionResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DependencyResolverImplTest {

    @Mock(strictness = LENIENT)
    Context<KafkaProxy> mockContext;

    @Test
    void testNoDependencies() {
        givenFiltersInContext();
        givenClusterRefsInContext();
        givenIngressesInContext();
        givenVirtualKafkaClustersInContext();

        // when
        ResolutionResult resolutionResult = DependencyResolverImpl.create().deepResolve(mockContext);

        // then
        assertThat(resolutionResult.allClustersInNameOrder()).isEmpty();
        assertThat(resolutionResult.clusterResults()).isEmpty();
        assertThat(resolutionResult.ingresses()).isEmpty();
        assertThat(resolutionResult.fullyResolvedClustersInNameOrder()).isEmpty();
        assertThat(resolutionResult.filter(filterRef("c"))).isEmpty();
        assertThat(resolutionResult.filters()).isEmpty();
    }

    @Test
    void testNullFiltersOnVirtualClusterTolerated() {
        givenFiltersInContext();
        givenClusterRefsInContext(kafkaServiceRef("cluster"));
        givenIngressesInContext();
        VirtualKafkaCluster cluster = virtualCluster(null, "cluster", List.of());
        givenVirtualKafkaClustersInContext(cluster);

        // when
        ResolutionResult resolutionResult = DependencyResolverImpl.create().deepResolve(mockContext);

        // then
        assertThat(resolutionResult.allClustersInNameOrder()).containsExactly(cluster);
        ClusterResolutionResult onlyResult = assertSingleResult(resolutionResult, cluster);
        assertThat(onlyResult.isFullyResolved()).isTrue();
        assertThat(onlyResult.unresolvedDependencySet()).isEmpty();
    }

    @Test
    void testSingleFilterUnreferenced() {
        KafkaProtocolFilter filter = protocolFilter("filterName");
        givenFiltersInContext(filter);
        givenClusterRefsInContext(kafkaServiceRef("cluster"));
        givenIngressesInContext();
        VirtualKafkaCluster cluster = virtualCluster(List.of(), "cluster", List.of());
        givenVirtualKafkaClustersInContext(cluster);

        // when
        ResolutionResult resolutionResult = DependencyResolverImpl.create().deepResolve(mockContext);

        // then
        assertThat(resolutionResult.filter(filterRef("filterName"))).contains(filter);
        assertThat(resolutionResult.filters()).containsExactly(filter);
        ClusterResolutionResult onlyResult = assertSingleResult(resolutionResult, cluster);
        assertThat(onlyResult.isFullyResolved()).isTrue();
        assertThat(onlyResult.unresolvedDependencySet()).isEmpty();
    }

    @Test
    void testSingleFilterReferenced() {
        KafkaProtocolFilter filter = protocolFilter("filterName");
        givenFiltersInContext(filter);
        givenClusterRefsInContext(kafkaServiceRef("cluster"));
        givenIngressesInContext();
        VirtualKafkaCluster cluster = virtualCluster(List.of(filterRef("filterName")), "cluster", List.of());
        givenVirtualKafkaClustersInContext(cluster);

        // when
        ResolutionResult resolutionResult = DependencyResolverImpl.create().deepResolve(mockContext);

        // then
        assertThat(resolutionResult.filter(filterRef("filterName"))).contains(filter);
        assertThat(resolutionResult.filters()).containsExactly(filter);
        ClusterResolutionResult onlyResult = assertSingleResult(resolutionResult, cluster);
        assertThat(onlyResult.isFullyResolved()).isTrue();
        assertThat(onlyResult.unresolvedDependencySet()).isEmpty();
    }

    @Test
    void testMultipleFiltersReferenced() {
        KafkaProtocolFilter filter = protocolFilter("filterName");
        KafkaProtocolFilter filter2 = protocolFilter("filterName2");
        givenFiltersInContext(filter, filter2);
        givenClusterRefsInContext(kafkaServiceRef("cluster"));
        givenIngressesInContext();
        VirtualKafkaCluster cluster = virtualCluster(List.of(filterRef("filterName"), filterRef("filterName2")), "cluster", List.of());
        givenVirtualKafkaClustersInContext(cluster);

        // when
        ResolutionResult resolutionResult = DependencyResolverImpl.create().deepResolve(mockContext);

        // then
        assertThat(resolutionResult.filter(filterRef("filterName"))).contains(filter);
        assertThat(resolutionResult.filter(filterRef("filterName2"))).contains(filter2);
        assertThat(resolutionResult.filters()).containsExactlyInAnyOrder(filter, filter2);
        ClusterResolutionResult onlyResult = assertSingleResult(resolutionResult, cluster);
        assertThat(onlyResult.isFullyResolved()).isTrue();
        assertThat(onlyResult.unresolvedDependencySet()).isEmpty();
    }

    @Test
    void testSubsetOfFiltersReferenced() {
        KafkaProtocolFilter filter = protocolFilter("filterName");
        givenFiltersInContext(filter);
        givenClusterRefsInContext(kafkaServiceRef("cluster"));
        givenIngressesInContext();
        VirtualKafkaCluster cluster = virtualCluster(List.of(filterRef("filterName"), filterRef("filterName2")), "cluster", List.of());
        givenVirtualKafkaClustersInContext(cluster);

        // when
        ResolutionResult resolutionResult = DependencyResolverImpl.create().deepResolve(mockContext);

        // then
        assertThat(resolutionResult.filter(filterRef("filterName"))).contains(filter);
        assertThat(resolutionResult.filter(filterRef("filterName2"))).isEmpty();
        assertThat(resolutionResult.filters()).containsExactlyInAnyOrder(filter);
        ClusterResolutionResult onlyResult = assertSingleResult(resolutionResult, cluster);
        assertThat(onlyResult.isFullyResolved()).isFalse();
        assertThat(onlyResult.unresolvedDependencySet()).containsExactly(new FilterRefBuilder().withName("filterName2").build());
    }

    @Test
    void testUnresolvedFilter() {
        KafkaProtocolFilter filter = protocolFilter("filterName");
        givenFiltersInContext(filter);
        givenClusterRefsInContext(kafkaServiceRef("clusterRef"));
        givenIngressesInContext();
        VirtualKafkaCluster cluster = virtualCluster(List.of(filterRef("other")), "clusterRef", List.of());
        givenVirtualKafkaClustersInContext(cluster);

        // when
        ResolutionResult resolutionResult = DependencyResolverImpl.create().deepResolve(mockContext);

        // then
        assertThat(resolutionResult.filter(filterRef("other"))).isEmpty();
        ClusterResolutionResult onlyResult = assertSingleResult(resolutionResult, cluster);
        assertThat(onlyResult.isFullyResolved()).isFalse();
        assertThat(onlyResult.unresolvedDependencySet()).containsExactly(new FilterRefBuilder().withName("other").build());
    }

    @Test
    void testUnresolvedIngress() {
        givenFiltersInContext();
        givenClusterRefsInContext(kafkaServiceRef("clusterRef"));
        givenIngressesInContext();
        VirtualKafkaCluster cluster = virtualCluster(List.of(), "clusterRef", List.of(ingressRef("ingressMissing")));
        givenVirtualKafkaClustersInContext(cluster);

        // when
        ResolutionResult resolutionResult = DependencyResolverImpl.create().deepResolve(mockContext);

        // then
        assertThat(resolutionResult.ingresses()).isEmpty();
        ClusterResolutionResult onlyResult = assertSingleResult(resolutionResult, cluster);
        assertThat(onlyResult.isFullyResolved()).isFalse();
        assertThat(onlyResult.unresolvedDependencySet()).containsExactly(new IngressRefBuilder().withName("ingressMissing").build());
    }

    @Test
    void testUnresolvedKafkaService() {
        givenFiltersInContext();
        givenClusterRefsInContext();
        givenIngressesInContext();
        VirtualKafkaCluster cluster = virtualCluster(List.of(), "missing", List.of());
        givenVirtualKafkaClustersInContext(cluster);

        // when
        ResolutionResult resolutionResult = DependencyResolverImpl.create().deepResolve(mockContext);

        // then
        assertThat(resolutionResult.ingresses()).isEmpty();
        ClusterResolutionResult onlyResult = assertSingleResult(resolutionResult, cluster);
        assertThat(onlyResult.isFullyResolved()).isFalse();
        assertThat(onlyResult.unresolvedDependencySet()).containsExactly(new KafkaServiceRefBuilder().withName("missing").build());
    }

    @Test
    void testSingleResolvedIngress() {
        givenFiltersInContext();
        givenClusterRefsInContext(kafkaServiceRef("clusterRef"));
        KafkaProxyIngress ingress = ingress("ingress");
        givenIngressesInContext(ingress);
        VirtualKafkaCluster cluster = virtualCluster(List.of(), "clusterRef", List.of(ingressRef("ingress")));
        givenVirtualKafkaClustersInContext(cluster);

        // when
        ResolutionResult resolutionResult = DependencyResolverImpl.create().deepResolve(mockContext);

        // then
        assertThat(resolutionResult.ingresses()).containsExactly(ingress);
        ClusterResolutionResult onlyResult = assertSingleResult(resolutionResult, cluster);
        assertThat(onlyResult.isFullyResolved()).isTrue();
        assertThat(onlyResult.unresolvedDependencySet()).isEmpty();
    }

    @Test
    void testMultipleResolvedIngresses() {
        givenFiltersInContext();
        givenClusterRefsInContext(kafkaServiceRef("clusterRef"));
        KafkaProxyIngress ingress = ingress("ingress");
        KafkaProxyIngress ingress2 = ingress("ingress2");
        givenIngressesInContext(ingress, ingress2);
        VirtualKafkaCluster cluster = virtualCluster(List.of(), "clusterRef", List.of(ingressRef("ingress"), ingressRef("ingress2")));
        givenVirtualKafkaClustersInContext(cluster);

        // when
        ResolutionResult resolutionResult = DependencyResolverImpl.create().deepResolve(mockContext);

        // then
        assertThat(resolutionResult.ingresses()).containsExactlyInAnyOrder(ingress, ingress2);
        ClusterResolutionResult onlyResult = assertSingleResult(resolutionResult, cluster);
        assertThat(onlyResult.isFullyResolved()).isTrue();
        assertThat(onlyResult.unresolvedDependencySet()).isEmpty();
    }

    @Test
    void testSubsetOfIngressesResolved() {
        givenFiltersInContext();
        givenClusterRefsInContext(kafkaServiceRef("clusterRef"));
        KafkaProxyIngress ingress = ingress("ingress");
        givenIngressesInContext(ingress);
        VirtualKafkaCluster cluster = virtualCluster(List.of(), "clusterRef", List.of(ingressRef("ingress"), ingressRef("ingress2")));
        givenVirtualKafkaClustersInContext(cluster);

        // when
        ResolutionResult resolutionResult = DependencyResolverImpl.create().deepResolve(mockContext);

        // then
        assertThat(resolutionResult.ingresses()).containsExactlyInAnyOrder(ingress);
        ClusterResolutionResult onlyResult = assertSingleResult(resolutionResult, cluster);
        assertThat(onlyResult.isFullyResolved()).isFalse();
        assertThat(onlyResult.unresolvedDependencySet()).containsExactly(new IngressRefBuilder().withName("ingress2").build());
    }

    private IngressRef ingressRef(String name) {
        return new IngressRefBuilder().withName(name).build();
    }

    private static KafkaProxyIngress ingress(String name) {
        return new KafkaProxyIngressBuilder().withNewMetadata().withName(name).endMetadata().build();
    }

    private static ClusterResolutionResult assertSingleResult(ResolutionResult resolutionResult, VirtualKafkaCluster cluster) {
        Collection<ClusterResolutionResult> result = resolutionResult.clusterResults();
        assertThat(result).hasSize(1);
        ClusterResolutionResult onlyResult = result.stream().findFirst().orElseThrow();
        assertThat(onlyResult.cluster()).isEqualTo(cluster);
        return onlyResult;
    }

    private static KafkaService kafkaServiceRef(String clusterRef) {
        return new KafkaServiceBuilder().withNewMetadata().withName(clusterRef).endMetadata().build();
    }

    private static FilterRef filterRef(String name) {
        return new FilterRefBuilder().withName(name).build();
    }

    private static KafkaProtocolFilter protocolFilter(String name) {
        return new KafkaProtocolFilterBuilder()
                .withApiVersion("filter.kroxylicious.io/v1alpha")
                .withKind("KafkaProtocolFilter")
                .withNewMetadata().withName(name)
                .endMetadata()
                .build();
    }

    private void givenFiltersInContext(KafkaProtocolFilter... resources) {
        givenSecondaryResourcesInContext(KafkaProtocolFilter.class, resources);
    }

    private void givenIngressesInContext(KafkaProxyIngress... ingresses) {
        givenSecondaryResourcesInContext(KafkaProxyIngress.class, ingresses);
    }

    private void givenVirtualKafkaClustersInContext(VirtualKafkaCluster... virtualKafkaClusters) {
        givenSecondaryResourcesInContext(VirtualKafkaCluster.class, virtualKafkaClusters);
    }

    private void givenClusterRefsInContext(KafkaService... clusterRefs) {
        givenSecondaryResourcesInContext(KafkaService.class, clusterRefs);
    }

    @SafeVarargs
    private <T> void givenSecondaryResourcesInContext(Class<T> type, T... resources) {
        when(mockContext.getSecondaryResources(type)).thenReturn(Arrays.stream(resources).collect(Collectors.toSet()));
    }

    private static VirtualKafkaCluster virtualCluster(List<FilterRef> filterRefs, String clusterRef, List<IngressRef> ingressRefs) {
        return new VirtualKafkaClusterBuilder()
                .withNewSpec()
                .withIngressRefs(ingressRefs)
                .withNewTargetKafkaServiceRef().withName(clusterRef).endTargetKafkaServiceRef()
                .withFilterRefs(filterRefs)
                .endSpec()
                .build();
    }

}
