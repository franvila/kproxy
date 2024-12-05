/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.systemtests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kroxylicious.systemtests.installation.kroxylicious.KroxyliciousOperator;

import static io.kroxylicious.systemtests.TestTags.OPERATOR;

@Tag(OPERATOR)
class OperatorST extends AbstractST {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorST.class);
    protected static KroxyliciousOperator kroxyliciousOperator;

    @Test
    void operatorInstallation() {
        kroxyliciousOperator = new KroxyliciousOperator(Constants.KO_NAMESPACE);
        kroxyliciousOperator.deployBundle();

        LOGGER.info("Deployed");

//        String bootstrap = kroxylicious.getBootstrap();
//
//        LOGGER.atInfo().setMessage("And a kafka Topic named {}").addArgument(topicName).log();
//        KafkaSteps.createTopic(namespace, topicName, bootstrap, 1, 2);
//
//        LOGGER.atInfo().setMessage("When {} messages '{}' are sent to the topic '{}'").addArgument(numberOfMessages).addArgument(MESSAGE).addArgument(topicName).log();
//        KroxyliciousSteps.produceMessages(namespace, topicName, bootstrap, MESSAGE, numberOfMessages);
//
//        LOGGER.atInfo().setMessage("Then the messages are consumed").log();
//        List<ConsumerRecord> result = KroxyliciousSteps.consumeMessages(namespace, topicName, bootstrap, numberOfMessages, Duration.ofMinutes(2));
//        LOGGER.atInfo().setMessage("Received: {}").addArgument(result).log();
//
//        assertThat(result).withFailMessage("expected messages have not been received!")
//                .extracting(ConsumerRecord::getValue)
//                .hasSize(numberOfMessages)
//                .allSatisfy(v -> assertThat(v).contains(MESSAGE));
    }

    @AfterEach
    void afterEach() {
        kroxyliciousOperator.delete();
    }
}
