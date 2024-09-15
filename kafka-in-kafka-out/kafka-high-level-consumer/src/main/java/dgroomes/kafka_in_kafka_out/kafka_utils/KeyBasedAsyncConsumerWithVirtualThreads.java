package dgroomes.kafka_in_kafka_out.kafka_utils;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An asynchronous Kafka consumer and message processor implemented with virtual threads. This is functionally similar
 * to {@link KeyBasedAsyncConsumerWithCoroutines}. Study the KDoc of that class for much more information.
 */
public class KeyBasedAsyncConsumerWithVirtualThreads<KEY, PAYLOAD> implements HighLevelConsumer {

    private static final Logger log = LoggerFactory.getLogger(KeyBasedAsyncConsumerWithVirtualThreads.class);
    private static final int QUEUE_DESIRED_MAX = 100;

    private final String topic;
    private final Duration pollDuration;
    private final Duration reportingDelay;
    private final Consumer<KEY, PAYLOAD> consumer;
    private final RecordProcessor<KEY, PAYLOAD> processFn;
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final AtomicInteger processed = new AtomicInteger(0);
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final Thread orchThread;
    private final Map<KEY, Future<?>> tailTaskByKey = new ConcurrentHashMap<>();
    private final Map<Integer, Queue<Future<ConsumerRecord<KEY, PAYLOAD>>>> ackQueuesByPartition = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private Instant nextReportingTime = Instant.now();

    public KeyBasedAsyncConsumerWithVirtualThreads(String topic, Duration pollDuration, Consumer<KEY, PAYLOAD> consumer, RecordProcessor<KEY, PAYLOAD> processFn, Duration reportingDelay) {
        this.topic = topic;
        this.consumer = consumer;
        this.processFn = processFn;
        this.pollDuration = pollDuration;
        this.reportingDelay = reportingDelay;

        // Needs to be an OS thread because virtual threads don't have priority and this thread is a high priority
        this.orchThread = new Thread(this::eventLoop);
        // "orch" is short for "orchestrator"
        this.orchThread.setName("consumer-orch");

        executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * (Non-blocking) Start the application
     */
    public void start() {
        active.getAndSet(true);
        consumer.subscribe(List.of(topic));
        orchThread.start();
    }

    /**
     * Continuously poll for records, schedule work, and commit offsets for finished work.
     */
    private void eventLoop() {
        while (true) {
            if (reportingDelay != null) {
                var now = Instant.now();
                if (nextReportingTime == null || now.isAfter(nextReportingTime)) {
                    log.info("Queue size: %,d Processed: %,d".formatted(queueSize.get(), processed.get()));
                    nextReportingTime = now.plus(reportingDelay);
                }
            }

            if (!active.get()) {
                log.info("The 'active' flag is false. Exiting the event loop.");
                break;
            }

            handleAcks();

            var size = queueSize.get();
            if (size > QUEUE_DESIRED_MAX) {
                log.debug("The desired maximum queue is full (%,d). Waiting for it to shrink...".formatted(size));
                unsafeRun(() -> Thread.sleep(pollDuration));
                continue;
            }

            intake();
        }
    }

    /**
     * Intake new work. Poll for records and schedule the work.
     */
    private void intake() {
        var records = consumer.poll(pollDuration);
        queueSize.getAndAdd(records.count());
        for (var record : records) {
            var key = record.key();
            var predecessorTask = tailTaskByKey.get(key);

            // Schedule the task and have it wait on the completion of its predecessor
            var task = executorService.submit(() -> {
                if (predecessorTask != null) {
                    unsafeRun(predecessorTask::get);
                }
                unsafeRun(() -> processFn.process(record));
            }, record);
            tailTaskByKey.put(key, task);

            var aQueue = ackQueuesByPartition.computeIfAbsent(record.partition(), k -> new LinkedBlockingQueue<>());
            aQueue.add(task);
        }
    }

    /**
     * Commit offsets ("acknowledgments") for finished work.
     */
    private void handleAcks() {
        var newOffsets = new HashMap<TopicPartition, OffsetAndMetadata>();

        for (var entry : ackQueuesByPartition.entrySet()) {
            var partition = entry.getKey();
            var queue = entry.getValue();
            long nextOffset = 0;

            while (true) {
                var future = queue.peek();
                if (future == null || !future.isDone()) break;

                queue.remove();
                queueSize.decrementAndGet();
                processed.incrementAndGet();

                var record = future.resultNow();
                nextOffset = record.offset() + 1;

                // Conditionally clean up the reference to the tail task for this key. Consider the following
                // scenarios:
                //
                //   - No new tasks have been scheduled for this key. The task is done. We can remove the reference.
                //   - A new task was scheduled for this key. The new task became the new tail. The new task
                //     completed. We can remove the reference.
                //   - A new task was scheduled, but it hasn't completed. We keep the reference.
                //
                var key = record.key();
                var tailTask = tailTaskByKey.get(key);
                if (tailTask != null && tailTask.isDone()) {
                    tailTaskByKey.remove(key);
                }
            }

            if (nextOffset > 0) newOffsets.put(new TopicPartition(topic, partition), new OffsetAndMetadata(nextOffset));
        }

        if (!newOffsets.isEmpty()) {
            consumer.commitAsync(newOffsets, (offsets, exception) -> {
                if (exception != null) log.error("Failed to commit offsets", exception);
            });
        }
    }

    @Override
    public void close() {
        log.info("Stopping...");
        active.getAndSet(false);
        unsafeRun(orchThread::join);
        consumer.close();
        executorService.shutdown();
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
