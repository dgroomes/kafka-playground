package dgroomes.virtual_thread_kafka_consumer;

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

/**
 * See the README for more information.
 */
public class KeyBasedAsyncConsumerWithVirtualThreads implements Closeable {

    private static final Logger log = LoggerFactory.getLogger("consumer");
    private static final int QUEUE_DESIRED_MAX = 100;

    @FunctionalInterface
    public interface RecordProcessor {

        void process(ConsumerRecord<String, String> record);
    }

    private final String topic;
    private final Duration pollDelay;
    private final Duration commitDelay;
    private final Consumer<String, String> consumer;
    private final RecordProcessor processFn;
    private int queueSize = 0;
    private final Map<Object, FutureRef> tailProcessTaskByKey = new HashMap<>();
    private final Map<Integer, FutureRef> tailOffsetTaskByPartition = new HashMap<>();
    private final Map<Integer, Long> nextOffsets = new HashMap<>();
    private final ExecutorService processExecutorService;
    private final ScheduledExecutorService orchExecutorService;

    public KeyBasedAsyncConsumerWithVirtualThreads(String topic, Duration pollDelay, Duration commitDelay, Consumer<String, String> consumer, RecordProcessor processor) {
        this.topic = topic;
        this.consumer = consumer;
        this.processFn = processor;
        this.pollDelay = pollDelay;
        this.commitDelay = commitDelay;

        // The orchestrator work needs to be backed by a separate platform thread (i.e. Operating System thread) than
        // the processor work so that orchestration work (polling, scheduling, offset committing) is
        // guaranteed time slices.
        //
        // If the orchestration work and processor work were all scheduled in virtual threads backed by the same
        // platform thread, then the orchestration work could be blocked by long-running processor work.
        //
        // "orch" is short for "orchestrator"
        orchExecutorService = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("consumer-orch").factory());

        // "proc" is short for "processor"
        processExecutorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("consumer-proc").factory());
    }

    /**
     * (Non-blocking) Start the application
     */
    public void start() {
        orchExecutorService.scheduleWithFixedDelay(this::poll, 0, pollDelay.toMillis(), TimeUnit.MILLISECONDS);
        orchExecutorService.scheduleWithFixedDelay(this::commit, 0, commitDelay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private static class FutureRef {
        Future<?> future;

        public void get() throws ExecutionException, InterruptedException {
            if (future != null) future.get();
        }
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

                // Schedule process work.
                var tailProcessTaskRef = tailProcessTaskByKey.get(key);
                var processTaskRef = new FutureRef();
                tailProcessTaskByKey.put(key, processTaskRef);
                processTaskRef.future = processExecutorService.submit(() -> {
                    log.debug("Processing record with key: {}", key);
                    if (tailProcessTaskRef != null) {

                        // Wait for the previous task of the same key to complete. This is our trick for getting in
                        // order processing by key.
                        unsafeRun(tailProcessTaskRef::get);
                    }

                    unsafeRun(() -> processFn.process(record));

                    // When processing is complete, we do some house cleaning and bookkeeping, but we need to do this in
                    // a thread safe way. So, we "confine" this work to the orchestrator thread. This is called
                    // "thread confinement". Alternatively, we could use locks, but the work we're doing doesn't need
                    // that level of control.
                    orchExecutorService.submit(() -> {
                        if (tailProcessTaskByKey.get(key) == processTaskRef) tailProcessTaskByKey.remove(key);
                        queueSize--;
                    });
                });

                // Schedule offset tracking work.
                var tailOffsetTask = tailOffsetTaskByPartition.get(partition);
                var offsetTaskRef = new FutureRef();
                tailOffsetTaskByPartition.put(partition, offsetTaskRef);
                offsetTaskRef.future = processExecutorService.submit(() -> {

                    // Similarly to how we get in order processing by key, we get in order offset tracking by partition.
                    // We have to wait for two tasks to complete: the processing task and the previous offset tracking
                    // task, if it exists.
                    unsafeRun(processTaskRef::get);
                    if (tailOffsetTask != null) unsafeRun(tailOffsetTask::get);
                    orchExecutorService.submit(() -> {
                        nextOffsets.put(partition, offset + 1);
                    });
                });
            }

            if (queueSize > QUEUE_DESIRED_MAX) {
                log.debug("Poll filled the queue (%,d) beyond the desired maximum size (%,d).".formatted(queueSize, QUEUE_DESIRED_MAX));
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
