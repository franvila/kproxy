/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.systemtests.resources;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.strimzi.api.kafka.model.common.Spec;
import io.strimzi.api.kafka.model.kafka.Status;
import io.strimzi.api.kafka.model.topic.KafkaTopic;

import io.kroxylicious.systemtests.Constants;
import io.kroxylicious.systemtests.enums.ConditionStatus;
import io.kroxylicious.systemtests.k8s.HelmClient;
import io.kroxylicious.systemtests.k8s.KubeClusterResource;
import io.kroxylicious.systemtests.resources.kubernetes.ClusterOperatorCustomResourceDefinition;
import io.kroxylicious.systemtests.resources.kubernetes.ClusterRoleBindingResource;
import io.kroxylicious.systemtests.resources.kubernetes.ClusterRoleResource;
import io.kroxylicious.systemtests.resources.kubernetes.ConfigMapResource;
import io.kroxylicious.systemtests.resources.kubernetes.DeploymentResource;
import io.kroxylicious.systemtests.resources.kubernetes.NamespaceResource;
import io.kroxylicious.systemtests.resources.kubernetes.RoleResource;
import io.kroxylicious.systemtests.resources.kubernetes.SecretResource;
import io.kroxylicious.systemtests.resources.kubernetes.ServiceAccountResource;
import io.kroxylicious.systemtests.resources.kubernetes.ServiceResource;
import io.kroxylicious.systemtests.resources.operator.BundleResource;
import io.kroxylicious.systemtests.resources.strimzi.KafkaNodePoolResource;
import io.kroxylicious.systemtests.resources.strimzi.KafkaResource;
import io.kroxylicious.systemtests.resources.strimzi.KafkaUserResource;

import static io.kroxylicious.systemtests.k8s.KubeClusterResource.cmdKubeClient;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The type Resource manager.
 */
public class ResourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManager.class);
    private static ResourceManager instance;
    private static String koDeploymentName = Constants.KO_DEPLOYMENT_NAME;
    private static ThreadLocal<ExtensionContext> testContext = new ThreadLocal<>();
    public static final Map<String, Stack<ResourceItem>> STORED_RESOURCES = new LinkedHashMap<>();

    private ResourceManager() {
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static synchronized ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    public static void setTestContext(ExtensionContext context) {
        testContext.set(context);
    }

    public static ExtensionContext getTestContext() {
        return testContext.get();
    }

    /**
     * Helm client.
     *
     * @return the helm client
     */
    public static HelmClient helmClient() {
        return KubeClusterResource.helmClusterClient();
    }

    private final ResourceType<?>[] resourceTypes = new ResourceType[]{
            new KafkaResource(),
            new KafkaUserResource(),
            new KafkaNodePoolResource(),
            new ServiceResource(),
            new ConfigMapResource(),
            new DeploymentResource(),
            new SecretResource(),
            new BundleResource(),
            new ClusterRoleBindingResource(),
            new RoleResource(),
            new ClusterRoleResource(),
            new NamespaceResource(),
            new ServiceAccountResource(),
            new ClusterOperatorCustomResourceDefinition()
    };

    public static String getKoDeploymentName() {
        return koDeploymentName;
    }

    public static void setKoDeploymentName(String newName) {
        koDeploymentName = newName;
    }

    /**
     * Create resource without wait.
     *
     * @param <T>   the type parameter
     * @param resources the resources
     */
    @SafeVarargs
    public final <T extends HasMetadata> void createResourceWithoutWait(T... resources) {
        createResource(false, resources);
    }

    /**
     * Create resource with wait.
     *
     * @param <T>    the type parameter
     * @param resources the resources
     */
    @SafeVarargs
    public final <T extends HasMetadata> void createResourceWithWait(T... resources) {
        createResource(true, resources);
    }

    @SafeVarargs
    private final <T extends HasMetadata> void createResource(boolean waitReady, T... resources) {
        for (T resource : resources) {
            ResourceType<T> type = findResourceType(resource);

            LOGGER.info("Creating/Updating {} {}",
                    resource.getKind(), resource.getMetadata().getName());

            assert type != null;
            type.create(resource);

            synchronized (this) {
                STORED_RESOURCES.computeIfAbsent(getTestContext().getDisplayName(), k -> new Stack<>());
                STORED_RESOURCES.get(getTestContext().getDisplayName()).push(
                        new ResourceItem<T>(
                                () -> deleteResource(resource),
                                resource));
            }
        }

        if (waitReady) {
            for (T resource : resources) {
                ResourceType<T> type = findResourceType(resource);
                if (Objects.equals(resource.getKind(), KafkaTopic.RESOURCE_KIND)) {
                    continue;
                }
                if (!waitResourceCondition(resource, ResourceCondition.readiness(type))) {
                    throw new RuntimeException(String.format("Timed out waiting for %s %s/%s to be ready", resource.getKind(), resource.getMetadata().getNamespace(),
                            resource.getMetadata().getName()));
                }
            }
        }
    }

    /**
     * Delete resource.
     *
     * @param <T> the type parameter
     * @param resources the resources
     */
    @SafeVarargs
    public final <T extends HasMetadata> void deleteResource(T... resources) {
        for (T resource : resources) {
            ResourceType<T> type = findResourceType(resource);

            if (type == null) {
                LOGGER.warn("Can't find resource type, please delete it manually");
                continue;
            }

            LOGGER.info("Deleting of {} {}/{}",
                    resource.getKind(), resource.getMetadata().getNamespace(), resource.getMetadata().getName());

            try {
                type.delete(resource);
                if (!waitResourceCondition(resource, ResourceCondition.deletion())) {
                    throw new RuntimeException(
                            String.format("Timed out deleting %s %s/%s", resource.getKind(), resource.getMetadata().getNamespace(), resource.getMetadata().getName()));
                }
            }
            catch (Exception e) {
                LOGGER.error("Failed to delete {} {}/{}", resource.getKind(), resource.getMetadata().getNamespace(), resource.getMetadata().getName(), e);
            }
        }
    }

    public void deleteResources() {
        LOGGER.info(String.join("", Collections.nCopies(76, "#")));
        if (!STORED_RESOURCES.containsKey(getTestContext().getDisplayName()) || STORED_RESOURCES.get(getTestContext().getDisplayName()).isEmpty()) {
            LOGGER.info("In context {} is everything deleted", getTestContext().getDisplayName());
        }
        else {
            LOGGER.info("Deleting all resources for {}", getTestContext().getDisplayName());
        }

        // if stack is created for specific test suite or test case
        AtomicInteger numberOfResources = STORED_RESOURCES.get(getTestContext().getDisplayName()) != null
                ? new AtomicInteger(STORED_RESOURCES.get(getTestContext().getDisplayName()).size())
                :
                // stack has no elements
                new AtomicInteger(0);
        while (STORED_RESOURCES.containsKey(getTestContext().getDisplayName()) && numberOfResources.get() > 0) {
            Stack<ResourceItem> stack = STORED_RESOURCES.get(getTestContext().getDisplayName());

            while (!stack.isEmpty()) {
                ResourceItem resourceItem = stack.pop();

                try {
                    resourceItem.getThrowableRunner().run();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                numberOfResources.decrementAndGet();
            }
        }
        STORED_RESOURCES.remove(getTestContext().getDisplayName());
        LOGGER.info(String.join("", Collections.nCopies(76, "#")));
    }

    public void deleteResourcesOfTypeWithoutWait(final String resourceKind) {
        // Delete all resources of the specified kind
        cmdKubeClient().deleteAllByResource(resourceKind);
        LOGGER.info("Deleted all resources of kind: {}", resourceKind);

        // Clear the instance stack of such resources
        STORED_RESOURCES
                .forEach((key, stack) -> stack.removeIf(resourceItem -> resourceItem.getResource() != null && resourceKind.equals(resourceItem.getResource().getKind())));
        LOGGER.info("Cleared all resources of kind: {} from the resource stack", resourceKind);
    }

    /**
     * Synchronizing all resources which are inside specific extension context.
     * @param <T> type of the resource which inherits from HasMetadata f.e Kafka, KafkaConnect, Pod, Deployment etc..
     */
    @SuppressWarnings(value = "unchecked")
    public final <T extends HasMetadata> void synchronizeResources() {
        Stack<ResourceItem> resources = STORED_RESOURCES.get(getTestContext().getDisplayName());

        // sync all resources
        for (ResourceItem resource : resources) {
            if (resource.getResource() == null) {
                continue;
            }
            ResourceType<T> type = findResourceType((T) resource.getResource());

            assert type != null;
            waitResourceCondition((T) resource.getResource(), ResourceCondition.readiness(type));
        }
    }

    /**
     * Wait resource condition.
     *
     * @param <T> the type parameter
     * @param resource the resource
     * @param condition the condition
     * @return the boolean
     */
    public final <T extends HasMetadata> boolean waitResourceCondition(T resource, ResourceCondition<T> condition) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(resource.getMetadata());
        Objects.requireNonNull(resource.getMetadata().getName());

        ResourceType<T> type = findResourceType(resource);
        assertNotNull(type);
        boolean[] resourceReady = new boolean[1];

        LOGGER.debug("Resource condition: {} to be fulfilled for resource {}: {}", condition.getConditionName(), resource.getKind(), resource.getMetadata().getName());
        await().atMost(ResourceOperation.getTimeoutForResourceReadiness(resource.getKind())).pollInterval(Constants.GLOBAL_POLL_INTERVAL)
                .until(() -> {
                    T res = type.get(resource.getMetadata().getNamespace(), resource.getMetadata().getName());
                    resourceReady[0] = condition.getPredicate().test(res);
                    if (!resourceReady[0]) {
                        type.delete(res);
                    }
                    return resourceReady[0];
                });

        return resourceReady[0];
    }

    @SuppressWarnings(value = "unchecked")
    private <T extends HasMetadata> ResourceType<T> findResourceType(T resource) {
        for (ResourceType<?> type : resourceTypes) {
            if (type.getKind().equals(resource.getKind())) {
                return (ResourceType<T>) type;
            }
        }
        return null;
    }

    /**
     * Wait until the CR is in desired state
     * @param <T> the type parameter
     * @param operation - client of CR - for example kafkaClient()
     * @param resource - custom resource
     * @param resourceTimeout the resource timeout
     * @return returns CR
     */
    public static <T extends CustomResource<? extends Spec, ? extends Status>> boolean waitForResourceStatusReady(MixedOperation<T, ?, ?> operation, T resource,
                                                                                                                  Duration resourceTimeout) {
        return waitForResourceStatusReady(operation, resource.getKind(), resource.getMetadata().getNamespace(), resource.getMetadata().getName(),
                ConditionStatus.TRUE, resourceTimeout);
    }

    /**
     * Wait for resource status.
     *
     * @param <T> the type parameter
     * @param operation the operation
     * @param kind the kind
     * @param namespace the namespace
     * @param name the name
     * @param resourceTimeout the resource timeout
     * @return the boolean
     */
    public static <T extends CustomResource<? extends Spec, ? extends Status>> boolean waitForResourceStatusReady(MixedOperation<T, ?, ?> operation, String kind,
                                                                                                                  String namespace, String name,
                                                                                                                  Duration resourceTimeout) {
        return waitForResourceStatusReady(operation, kind, namespace, name, ConditionStatus.TRUE, resourceTimeout);
    }

    /**
     * Wait for resource status.
     *
     * @param <T> the type parameter
     * @param operation the operation
     * @param kind the kind
     * @param namespace the namespace
     * @param name the name
     * @param conditionStatus the condition status
     * @param resourceTimeout the resource timeout
     * @return the boolean
     */
    public static <T extends CustomResource<? extends Spec, ? extends Status>> boolean waitForResourceStatusReady(MixedOperation<T, ?, ?> operation, String kind,
                                                                                                                  String namespace, String name,
                                                                                                                  ConditionStatus conditionStatus,
                                                                                                                  Duration resourceTimeout) {
        LOGGER.info("Waiting for {}: {}/{} will have desired state 'Ready'", kind, namespace, name);
        await().atMost(resourceTimeout).pollInterval(Constants.POLL_INTERVAL_FOR_RESOURCE_READINESS)
                .until(() -> {
                    final Status status = operation.inNamespace(namespace)
                            .withName(name)
                            .get()
                            .getStatus();
                    if (status != null) {
                        return status.getConditions().stream()
                                .anyMatch(condition -> condition.getType().equals("Ready") && condition.getStatus().toUpperCase().equals(conditionStatus.toString()));
                    }
                    return false;
                });

        LOGGER.info("{}: {}/{} is in desired state 'Ready'", kind, namespace, name);
        return true;
    }

    /**
     * Wait for resource status ready.
     *
     * @param <T>  the type parameter
     * @param operation the operation
     * @param resource the resource
     * @return the boolean
     */
    public static <T extends CustomResource<? extends Spec, ? extends Status>> boolean waitForResourceStatusReady(MixedOperation<T, ?, ?> operation, T resource) {
        Duration resourceTimeout = ResourceOperation.getTimeoutForResourceReadiness(resource.getKind());
        return waitForResourceStatusReady(operation, resource, resourceTimeout);
    }
}