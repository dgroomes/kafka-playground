package dgroomes.kafka_in_kafka_out.kafka_utils;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This synchronously processes every batch of records received via {@link Consumer#poll(Duration)}.
 */
public class KeyBasedSyncConsumer<KEY, PAYLOAD> implements HighLevelConsumer {

    private static final Logger log = LoggerFactory.getLogger("consumer.sync");

    private final String topic;
    private final Consumer<KEY, PAYLOAD> consumer;
    private final Duration pollDuration;
    private final RecordProcessor<KEY, PAYLOAD> recordProcessor;
    private final Duration reportingDelay;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private Thread eventThread;
    private final AtomicInteger processing = new AtomicInteger(0);
    private final AtomicInteger processed = new AtomicInteger(0);
    private Duration processingTime = Duration.ZERO;
    private final ScheduledExecutorService reportExecutor;

    public KeyBasedSyncConsumer(String topic, Duration pollDuration, Consumer<KEY, PAYLOAD> consumer, RecordProcessor<KEY, PAYLOAD> recordProcessor, Duration reportingDelay) {
        this.topic = topic;
        this.pollDuration = pollDuration;
        this.consumer = consumer;
        this.recordProcessor = recordProcessor;
        this.reportingDelay = reportingDelay;
        reportExecutor = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("reporter").factory());
    }

    /**
     * (Non-blocking) Start the application
     */
    public void start() {
        active.getAndSet(true);
        consumer.subscribe(List.of(topic));
        eventThread = Thread.ofPlatform().name("consumer").start(this::pollContinuously);
        reportExecutor.scheduleAtFixedRate(this::report, 0, reportingDelay.toMillis(), TimeUnit.MILLISECONDS);
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
                processing.set(count);

                var start = Instant.now();

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
                processing.set(0);
                processed.getAndAdd(count);
                processingTime = processingTime.plus(Duration.between(start, Instant.now()));
                log.debug("Processed %,d records".formatted(processed.get()));
            }
        } catch (WakeupException e) {
            // Ignore exception if inactive because this is expected from the "stop" method
            if (active.get()) {
                log.info("rethrowing");
                throw e;
            }
        }
    }

    private void report() {
        var pTime = processingTime;
        var pCount = processed.get();
        var messagesPerSecond = pTime.isZero() ? 0.0 : (pCount * 1.0e9) / pTime.toNanos();

        log.info("Processing: %,10d\tProcessed: %,10d\tMessages per second: %10.2f".formatted(processing.get(), pCount, messagesPerSecond));
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
