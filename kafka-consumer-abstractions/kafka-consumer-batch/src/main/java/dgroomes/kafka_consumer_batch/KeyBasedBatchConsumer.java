package dgroomes.kafka_consumer_batch;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This processes every batch of records received via {@link Consumer#poll(Duration)}. This is a "process the batch to completion"
 * style.
 */
public class KeyBasedBatchConsumer implements Closeable {

    private static final Logger log = LoggerFactory.getLogger("consumer.batch");

    @FunctionalInterface
    public interface RecordProcessor {

        void process(ConsumerRecord<String, String> record);
    }

    private final Consumer<String, String> consumer;
    private final Duration pollDuration;
    private final RecordProcessor recordProcessor;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private Thread eventThread;

    public KeyBasedBatchConsumer(Duration pollDuration, Consumer<String, String> consumer, RecordProcessor recordProcessor) {
        this.pollDuration = pollDuration;
        this.consumer = consumer;
        this.recordProcessor = recordProcessor;
    }

    /**
     * (Non-blocking) Start the application
     */
    public void start() {
        active.getAndSet(true);
        eventThread = Thread.ofPlatform().name("consumer").start(this::pollContinuously);
    }

    public void pollContinuously() {
        try {
            while (true) {
                if (!active.get()) {
                    log.info("The 'active' flag is false. Exiting the event loop.");
                    break;
                }

                var records = consumer.poll(pollDuration);
                var count = records.count();
                if (count == 0) {
                    continue;
                }

                log.debug("Poll received %,d records".formatted(count));

                record TopicPartitionKey(String topic, int partition, Object key) {}

                // Similar to the "async" consumers in this project, we'll implement partition-key based processing.
                // Units of work not related to each other will be processed in parallel.
                var groups = StreamSupport.stream(records.spliterator(), false)
                        .collect(Collectors.groupingBy(record ->
                                new TopicPartitionKey(record.topic(), record.partition(), record.key())))
                        .values();

                log.debug("Separated %,d groups of records".formatted(groups.size()));

                groups
                        .parallelStream()
                        .forEach(group -> group.forEach(recordProcessor::process));

                consumer.commitAsync();
                log.debug("Processed %,d records".formatted(count));
            }
        } catch (WakeupException e) {
            // Ignore exception if inactive because this is expected from the "stop" method
            if (active.get()) {
                log.info("rethrowing");
                throw e;
            }
        }
    }

    @Override
    public void close() {
        // TODO revisit this.
        log.info("Stopping...");
        active.getAndSet(false);
        consumer.wakeup();
        try {
            eventThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        consumer.close();
    }
}
