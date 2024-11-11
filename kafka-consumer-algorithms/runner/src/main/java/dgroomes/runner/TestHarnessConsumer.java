package dgroomes.runner;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static dgroomes.runner.TestHarness.*;

/**
 * A simple Kafka consumer abstraction tailored for the test harness. It's extension point is the 'recordProcessor'
 * function.
 */
class TestHarnessConsumer {

    private static final Logger log = LoggerFactory.getLogger("runner");

    private final Consumer<String, String> consumer;
    private final AtomicBoolean active = new AtomicBoolean(false);

    TestHarnessConsumer() {
        this.consumer = new KafkaConsumer<>(Map.of("bootstrap.servers", BROKER_HOST,
                "key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer",
                "value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));
    }

    void seekToEnd() {
        if (active.get()) {
            throw new IllegalStateException("The consumer is already started. It is an error to seek when it is already started.");
        }

        // Assign the consumer to the topic partitions and seek to the end of those topic partitions.
        var partitionInfos = consumer.partitionsFor(OUTPUT_TOPIC);
        var topicPartitions = partitionInfos.stream()
                .map(it -> new TopicPartition(it.topic(), it.partition()))
                .collect(Collectors.toList());
        consumer.assign(topicPartitions);
        consumer.seekToEnd(topicPartitions); // Warning: this is lazy
        // Calling 'position' will force the consumer to actually do the "seek to end" operation.
        topicPartitions.forEach(partition -> {
            long position = consumer.position(partition);
            log.debug("Partition: {}, Offset: {}", partition, position);
        });
    }

    private class PollNextSession {

        private final ScheduledExecutorService reportExecutor;
        CompletableFuture<List<ConsumerRecord<String, String>>> future = new CompletableFuture<>();
        private final int desiredCount;
        private final boolean report;
        private final AtomicInteger completed = new AtomicInteger(0);
        private final Instant start = Instant.now();
        private final AtomicReference<Duration> cumulativeWaitTime = new AtomicReference<>(Duration.ZERO);

        private PollNextSession(int desiredCount, boolean report) {
            this.desiredCount = desiredCount;
            this.report = report;
            Thread.ofPlatform().name("consumer").start(this::pollLoop);
            reportExecutor = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("reporter").factory());
            if (report) {
                reportExecutor.scheduleAtFixedRate(this::report, 0, REPORTING_DELAY.toMillis(), TimeUnit.MILLISECONDS);
                log.info(String.format("%-11s  %-8s  %-13s  %-13s", "Processed", "Elapsed", "Throughput", "Avg. Latency"));
            }
        }

        void pollLoop() {
            var allRecords = new ArrayList<ConsumerRecord<String, String>>();
            try {
                while (true) {
                    if (!active.get()) {
                        log.debug("The 'active' flag is false. Exiting the event loop.");
                        break;
                    }

                    var records = consumer.poll(POLL_TIMEOUT);
                    var foundCount = records.count();
                    if (foundCount == 0) continue;

                    log.debug("Poll received %,d records".formatted(foundCount));
                    for (var record : records) {
                        allRecords.add(record);
                        var waitTime = Duration.between(start, Instant.ofEpochMilli(record.timestamp()));
                        cumulativeWaitTime.updateAndGet(it -> it.plus(waitTime));
                    }

                    if (completed.addAndGet(foundCount) >= desiredCount) {
                        log.debug("Desired count reached. Completing the future.");
                        active.set(false);
                        future.complete(allRecords);
                        break;
                    }
                }
            } catch (KafkaException e) {
                // Ignore exception if inactive because this is the expected shutdown path.
                if (!active.get()) return;
                future.completeExceptionally(e);
            } finally {
                active.set(false);
                reportExecutor.shutdown();
                if (report) report();
            }
        }

        void report() {
            var processed = completed.get();
            var elapsed = Duration.between(start, Instant.now());
            var cumulativeLatency = this.cumulativeWaitTime.get();

            var processedStr = "%,d msgs".formatted(processed);
            var throughput = (elapsed.isZero()) ? "-" : "%.2f msg/s".formatted((processed * 1.0e9) / elapsed.toNanos());
            var avgLatency = processed == 0 ? "-" : "%.2f s".formatted(cumulativeLatency.dividedBy(processed).toNanos() / 1e9);
            var elapsedTime = "%.2f s".formatted(elapsed.toNanos() / 1e9);

            log.info("%-11s  %-8s  %-13s  %-13s".formatted(processedStr, elapsedTime, throughput, avgLatency));
        }
    }

    List<ConsumerRecord<String, String>> pollNext(int count, boolean report) {
        log.debug("Polling for %,d records ...".formatted(count));
        if (!active.compareAndSet(false, true)) {
            throw new IllegalStateException("There is already an active 'pollNext' request. It is an error to start another one.");
        }

        var session = new PollNextSession(count, report);
        // In isolation, a given message will take a second or two to process (totally depends on the speed of the
        // machine though). So let's assume it takes a maximum of 3 seconds to process a message.
        var timeout = count * 3L;
        try {
            return session.future.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Timed out after %s seconds waiting for %,d records in the 'pollNext' request".formatted(timeout, count), e);
        }
    }

    void stop() {
        log.debug("Stopping...");
        active.getAndSet(false);
        consumer.wakeup();
        consumer.close();
    }
}
