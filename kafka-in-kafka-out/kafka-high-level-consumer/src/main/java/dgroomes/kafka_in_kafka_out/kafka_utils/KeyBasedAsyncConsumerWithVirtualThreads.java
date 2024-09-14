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

import static java.util.concurrent.Future.State.*;

/**
 * This asynchronously processes messages. Works is scheduled with virtual threads. Compare with {@link SyncConsumer}.
 * <p>
 * Messages are processed in "key order". This means that a message with a key of "xyz" will always be processed before
 * a later message with a key of "xyz". Messages with the same key are assumed to be on the same partition.
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
    private final Thread eventLoopThread;
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
        this.eventLoopThread = new Thread(this::eventLoop);

        executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * (Non-blocking) Start the application
     */
    public void start() {
        active.getAndSet(true);
        consumer.subscribe(List.of(topic));
        eventLoopThread.start();
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
                try {
                    Thread.sleep(pollDuration);
                } catch (InterruptedException e) {
                    log.info("Interrupted while waiting for the landing zone to clear. Continuing...");
                    continue;
                }
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
                    try {
                        predecessorTask.get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Something went wrong while waiting for the predecessor task to complete", e);
                        // You need to choose how to handle this. Dead letter queue? Retry? Skip? And you need
                        // to choose where to handle this.
                    }
                }
                try {
                    processFn.process(record);
                } catch (Exception e) {
                    log.error("Something went wrong while processing a record", e);
                    // You need to choose how to handle this. Dead letter queue? Retry? Skip?
                }
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
                if (future == null) break;

                var state = future.state();
                if (state == RUNNING) break;

                queue.remove();
                queueSize.decrementAndGet();
                processed.incrementAndGet();

                if (state == CANCELLED || state == FAILED) {
                    log.error("A task failed or was cancelled. This is not expected. You need to decide how to handle this.");
                } else if (state == SUCCESS) {
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
                } else {
                    log.error("Unexpected state: {}", state);
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
        // TODO wait for buffer to flush maybe?
        log.info("Stopping...");
        active.getAndSet(false);
        consumer.wakeup();
        try {
            eventLoopThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        consumer.close();
        executorService.shutdown();
        log.info("Stopped.");
    }
}
