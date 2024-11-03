package dgroomes.kafka_in_kafka_out.kafka_utils;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * An asynchronous Kafka consumer and message processor implemented with virtual threads. This is functionally similar
 * to {@link KeyBasedAsyncConsumerWithCoroutines}. Study the KDoc of that class for much more information.
 */
public class KeyBasedAsyncConsumerWithVirtualThreads<KEY, PAYLOAD> implements HighLevelConsumer {

    private static final Logger log = LoggerFactory.getLogger("consumer.virtual-threads");
    private static final int QUEUE_DESIRED_MAX = 100;

    private final String topic;
    private final Duration pollDelay;
    private final Duration commitDelay;
    private final Duration reportingDelay;
    private final Consumer<KEY, PAYLOAD> consumer;
    private final RecordProcessor<KEY, PAYLOAD> processFn;
    private int queueSize = 0;
    private int processed = 0;
    private final Map<KEY, Future<?>> tailProcessTaskByKey = new HashMap<>();
    private final Map<Integer, Future<?>> tailOffsetTaskByPartition = new HashMap<>();
    private final Map<Integer, Long> nextOffsets = new HashMap<>();
    private final ExecutorService processExecutorService;
    private final ScheduledExecutorService orchExecutorService;

    public KeyBasedAsyncConsumerWithVirtualThreads(String topic, Duration pollDelay, Duration commitDelay, Duration reportingDelay, Consumer<KEY, PAYLOAD> consumer, RecordProcessor<KEY, PAYLOAD> processFn) {
        this.topic = topic;
        this.consumer = consumer;
        this.processFn = processFn;
        this.reportingDelay = reportingDelay;
        this.pollDelay = pollDelay;
        this.commitDelay = commitDelay;

        // The orchestrator threads needs to be an OS thread so it is guaranteed time slices unlike virtual threads
        // which may be blocked by other long-running CPU-intensive virtual threads.
        orchExecutorService = Executors.newSingleThreadScheduledExecutor(r -> {
            // "orch" is short for "orchestrator"
            return new Thread(r, "consumer-orch");
        });

        processExecutorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * (Non-blocking) Start the application
     */
    public void start() {
        consumer.subscribe(List.of(topic));
        orchExecutorService.scheduleWithFixedDelay(this::poll, 0, pollDelay.toMillis(), TimeUnit.MILLISECONDS);
        orchExecutorService.scheduleWithFixedDelay(this::commit, 0, commitDelay.toMillis(), TimeUnit.MILLISECONDS);
        orchExecutorService.scheduleWithFixedDelay(this::report, 0, reportingDelay.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Poll for records and schedule the work.
     */
    private void poll() {
        if (queueSize > QUEUE_DESIRED_MAX) {
            log.debug("The desired maximum queue is full (%,d). Skipping poll.".formatted(queueSize));
            return;
        }

        while (true) {
            var records = consumer.poll(Duration.ZERO);
            log.debug("Polled {} records", records.count());
            if (records.isEmpty()) return;
            queueSize += records.count();

            for (var record : records) {
                var partition = record.partition();
                var offset = record.offset();
                var key = record.key();

                // Schedule handler work.
                var tailProcessTask = tailProcessTaskByKey.get(key);
                var processTask = processExecutorService.submit(() -> {
                    if (tailProcessTask != null) {

                        // Wait for the previous task of the same key to complete. This is our trick for getting in
                        // order process by key.
                        unsafeRun(tailProcessTask::get);
                    }

                    unsafeRun(() -> processFn.process(record));
                });
                tailProcessTaskByKey.put(key, processTask);

                // Schedule clean up task. Clean up the reference to the tail job, unless another job has taken its
                // place.
                orchExecutorService.submit(() -> {
                    unsafeRun(processTask::get);
                    if (tailProcessTaskByKey.get(key) == processTask) tailProcessTaskByKey.remove(key);
                });

                // Schedule offset tracking work.
                var tailOffsetTask = tailOffsetTaskByPartition.get(partition);
                orchExecutorService.submit(() -> {
                    if (tailOffsetTask != null) unsafeRun(tailOffsetTask::get);
                    nextOffsets.put(partition, offset + 1);
                    processed++;
                    queueSize--;
                });
            }

            if (queueSize > QUEUE_DESIRED_MAX) {
                log.debug("Poll filled the queue (%,d) beyond beyond the desired maximum size (%,d).".formatted(queueSize, QUEUE_DESIRED_MAX));
                return;
            }
        }
    }

    /**
     * Commit offsets for finished work.
     */
    private void commit() {
        if (nextOffsets.isEmpty()) return;

        var newOffsets = new HashMap<TopicPartition, OffsetAndMetadata>();
        for (var entry : nextOffsets.entrySet()) {
            var partition = entry.getKey();
            var offset = entry.getValue();
            newOffsets.put(new TopicPartition(topic, partition), new OffsetAndMetadata(offset));
        }

        consumer.commitAsync(newOffsets, null);
    }

    private void report() {
        log.info("Queue size: %,d Processed: %,d".formatted(queueSize, processed));
    }

    @Override
    public void close() {
        log.info("Stopping...");
        processExecutorService.shutdownNow();
        orchExecutorService.shutdownNow();
        log.info("Stopped.");
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
}
