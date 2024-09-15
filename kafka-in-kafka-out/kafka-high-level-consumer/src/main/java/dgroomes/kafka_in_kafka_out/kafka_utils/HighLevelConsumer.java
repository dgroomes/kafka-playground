package dgroomes.kafka_in_kafka_out.kafka_utils;

import org.apache.kafka.clients.consumer.Consumer;

import java.io.Closeable;
import java.time.Duration;

/**
 * A high-level Kafka consumer. It owns the "poll" loop and encapsulates the mechanics of scheduling work, and commiting
 * offsets.
 */
public interface HighLevelConsumer extends Closeable {

    void start();

    static <KEY, PAYLOAD> HighLevelConsumer syncConsumer(String topic, Duration pollDuration, Consumer<KEY, PAYLOAD> kafkaConsumer, RecordProcessor<KEY, PAYLOAD> recordProcessor) {
        return new SyncConsumer<>(topic, pollDuration, kafkaConsumer, recordProcessor);
    }

    static <KEY, PAYLOAD> HighLevelConsumer asyncConsumerVirtualThreads(String topic, Duration pollDuration, Duration commitDelay, Duration reportingDelay, Consumer<KEY, PAYLOAD> kafkaConsumer, RecordProcessor<KEY, PAYLOAD> recordProcessor) {
        return new KeyBasedAsyncConsumerWithVirtualThreads<>(topic, pollDuration, commitDelay, reportingDelay, kafkaConsumer, recordProcessor);
    }

    static <KEY, PAYLOAD> HighLevelConsumer asyncConsumerCoroutines(String topic, Duration pollDuration, Consumer<KEY, PAYLOAD> kafkaConsumer, SuspendingRecordProcessor<KEY, PAYLOAD> recordProcessor, Duration reportingDelay, Duration commitDelay) {
        return new KeyBasedAsyncConsumerWithCoroutines<>(topic, pollDuration, kafkaConsumer, recordProcessor, reportingDelay, commitDelay);
    }
}
