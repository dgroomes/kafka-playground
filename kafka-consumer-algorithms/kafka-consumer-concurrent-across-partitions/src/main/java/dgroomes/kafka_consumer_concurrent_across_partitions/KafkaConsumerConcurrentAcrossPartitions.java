package dgroomes.kafka_consumer_concurrent_across_partitions;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * See the README for more information.
 */
public class KafkaConsumerConcurrentAcrossPartitions implements Closeable {
    private static final Logger log = LoggerFactory.getLogger("consumer");
    private static final int IN_FLIGHT_DESIRED_MAX = 100;

    @FunctionalInterface
    public interface RecordProcessor {
        void process(ConsumerRecord<String, String> record);
    }

    private final Duration pollDelay;
    private final Duration commitDelay;
    private final Consumer<String, String> consumer;
    private final RecordProcessor processor;
    private final Map<TopicPartition, Future<?>> tailTaskByPartition = new HashMap<>();
    private final Map<TopicPartition, Long> nextOffsets = new HashMap<>();
    private final ScheduledExecutorService orchExecutor;
    private final ExecutorService processorExecutor;
    private int inFlight = 0;

    public KafkaConsumerConcurrentAcrossPartitions(
            Duration pollDelay,
            Duration commitDelay,
            Consumer<String, String> consumer,
            RecordProcessor processor) {
        this.pollDelay = pollDelay;
        this.commitDelay = commitDelay;
        this.consumer = consumer;
        this.processor = processor;

        // The orchestrator work needs to be backed by a separate thread pool than the processor work so that the
        // orchestration work (polling, scheduling, offset committing) is never starved of a thread.
        this.orchExecutor = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("consumer-orchestrator").factory());
        this.processorExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("consumer-processor").factory());
    }

    public void start() {
        orchExecutor.scheduleWithFixedDelay(this::poll, 0, pollDelay.toMillis(), TimeUnit.MILLISECONDS);
        orchExecutor.scheduleWithFixedDelay(this::commit, 0, commitDelay.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Poll for records and schedule the work.
     */
    private void poll() {
        if (inFlight > IN_FLIGHT_DESIRED_MAX) {
            log.debug("The in-flight work (%,d) exceeds the desired maximum (%,d). Skipping poll.".formatted(inFlight, IN_FLIGHT_DESIRED_MAX));
            return;
        }

        while (true) {
            var records = consumer.poll(Duration.ZERO);
            if (records.isEmpty()) break;

            inFlight += records.count();
            log.debug("Polled {} records", records.count());

            for (var partition : records.partitions()) {
                var partitionRecords = records.records(partition);
                if (partitionRecords.isEmpty()) continue;
                var tailTask = tailTaskByPartition.get(partition);
                var task = processorExecutor.submit(() -> {
                    log.debug("Processing {} records for partition {} starting at offset {}", partitionRecords.size(), partition, partitionRecords.getFirst().offset());

                    // Wait for the previous task of the same partition to complete. This is our trick for getting in
                    // order processing.
                    if (tailTask != null) unsafeRun(tailTask::get);

                    partitionRecords.forEach(processor::process);

                    // When processing is complete, we need to move forward the offsets, and we need to do it in a
                    // thread safe way. So, we "confine" this work to the orchestrator thread. This is called "thread
                    // confinement". Alternatively, we could use locks, but we don't need that level of control.
                    orchExecutor.submit(() -> {
                        var nextOffset = partitionRecords.getLast().offset() + 1;
                        nextOffsets.put(partition, nextOffset);
                        inFlight--;
                    });
                });
                tailTaskByPartition.put(partition, task);
            }

            if (inFlight > IN_FLIGHT_DESIRED_MAX) {
                log.debug("Poll pushed in-flight work (%,d) beyond the desired maximum (%,d).".formatted(inFlight, IN_FLIGHT_DESIRED_MAX));
                return;
            }
        }
    }

    private void commit() {
        if (nextOffsets.isEmpty()) return;

        var offsetsToCommit = nextOffsets.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new OffsetAndMetadata(entry.getValue())
        ));

        consumer.commitAsync(offsetsToCommit, null);
        nextOffsets.clear();
    }

    interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * This toy implementation of a Kafka consumer does not handle errors. In a production implementation, you need to
     * take care of errors.
     */
    private static void unsafeRun(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("[unsafeRun] Squashed error", e);
        }
    }

    @Override
    public void close() {
        log.info("Stopping...");
        processorExecutor.shutdownNow();
        orchExecutor.shutdownNow();
        log.info("Stopped.");
    }
}
