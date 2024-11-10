package dgroomes.kafka_consumer_concurrent_across_partitions_within_same_poll;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * See the README for more information.
 */
public class KafkaConsumerConcurrentAcrossPartitionsWithinSamePoll implements Closeable {

    private static final Logger log = LoggerFactory.getLogger("consumer");

    @FunctionalInterface
    public interface RecordProcessor {

        void process(ConsumerRecord<String, String> record);
    }

    private final Consumer<String, String> consumer;
    private final Duration pollDuration;
    private final RecordProcessor recordProcessor;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final Thread eventThread;
    private final ExecutorService executor;

    public KafkaConsumerConcurrentAcrossPartitionsWithinSamePoll(Duration pollDuration, Consumer<String, String> consumer, RecordProcessor recordProcessor) {
        this.pollDuration = pollDuration;
        this.consumer = consumer;
        this.recordProcessor = recordProcessor;
        eventThread = Thread.ofPlatform().name("consumer").unstarted(this::pollContinuously);
        executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("consumer-processor").factory());
    }

    public void start() {
        if (!active.compareAndSet(false, true)) {
            throw new IllegalStateException("The consumer was already started. It is an error to start a consumer that is already started.");
        }

        eventThread.start();
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

                var futures = records.partitions().stream()
                        .map(partition -> {
                            var partitionRecords = records.records(partition);
                            return executor.submit(() -> partitionRecords.forEach(recordProcessor::process));
                        })
                        .toList();

                futures.forEach(t -> {
                    try {
                        t.get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Unexpected error occurred.", e);
                    }
                });

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
        log.info("Stopping...");
        active.getAndSet(false);
        consumer.wakeup();
        try {
            eventThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        consumer.close();
        executor.shutdown();
    }
}
