/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kafka.transform;

import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;

@FunctionalInterface
public interface RecordConsumer<S> {

    void accept(RecordBatch batch, Record record, S state);
}
