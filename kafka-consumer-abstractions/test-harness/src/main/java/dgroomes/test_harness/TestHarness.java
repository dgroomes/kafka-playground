package dgroomes.test_harness;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static dgroomes.test_harness.TestHarness.*;

/**
 * A test harness for exercising the "kafka-consumer-abstractions" application via different scenarios and load simulations. This
 * program sends Kafka messages to the input topic ('input') and may read messages from the output topic ('output')
 * to verify correctness.
 */
public class TestHarness {

    String[] args;
    Logger log = LoggerFactory.getLogger("test-harness");
    static String BROKER_HOST = "localhost:9092";
    static String INPUT_TOPIC = "input";
    static String OUTPUT_TOPIC = "output";
    static Duration POLL_TIMEOUT = Duration.ofMillis(250);
    static Duration QUICK_TAKE_TIMEOUT = Duration.ofSeconds(3);
    static Duration REPORTING_DELAY = Duration.ofSeconds(2);

    TestHarnessConsumer consumer;
    KafkaProducer<String, String> producer;

    public TestHarness(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) {
        new TestHarness(args).run();
    }

    void run() {
        if (args.length == 0) {
            log.error("No arguments found. You must specify a test/simulation scenario: 'one-message', 'multi-message', or 'load'");
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("shutdown-hook").unstarted(() -> Optional.ofNullable(consumer).ifPresent(TestHarnessConsumer::stop)));

        consumer = new TestHarnessConsumer();
        consumer.seekToEnd();

        // Initialize the producer
        {
            var props = Map.<String, Object>of("bootstrap.servers", BROKER_HOST,
                    "key.serializer", "org.apache.kafka.common.serialization.StringSerializer",
                    "value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            producer = new KafkaProducer<>(props);
        }

        var scenario = args[0];
        switch (scenario) {
            case "one-message" -> oneMessage();
            case "multi-message" -> multiMessage();
            case "load" -> load();
            case "load-batchy" -> loadBatchy();
            default -> {
                log.error("Unknown scenario: '{}'", scenario);
                System.exit(1);
            }
        }
    }

    /**
     * Send a message to the input topic and then poll for the message on the output topic. The
     * transformed message should be the second prime number (3).
     */
    void oneMessage() {
        log.info("SCENARIO: Single message test");

        // Let's make the Kafka message unique so that we reduce the chances of crossing wires and accidentally creating
        // a false positive/negative test result. We can use the epoch second as the record key.
        var key = String.valueOf(Instant.now().getEpochSecond());
        producer.send(new ProducerRecord<>(INPUT_TOPIC, key, String.valueOf(2)));
        producer.flush();

        var foundAll = consumer.pollNext(1, false, QUICK_TAKE_TIMEOUT);
        if (foundAll.size() > 1) {
            log.error("Fail: Expected at most one record but found {}", foundAll.size());
            return;
        }

        var found = foundAll.getFirst();
        if (!found.key().equals(key)) {
            log.error("Fail: The key was not what we expected. Expected: {}, but found: {}", key, found.key());
            return;
        }

        var expected = "The 2 prime number is 3";
        if (!expected.equals(found.value())) {
            log.error("""
                    FAIL: The message was not what we expected. Expected:
                    
                    {}
                    
                    But found:
                    
                    {}""", expected, found.value());
            return;
        }

        log.info("SUCCESS: Found the message we expected");
    }

    /**
     * Send multiple messages of the same key, and messages to multiple partitions.
     */
    void multiMessage() {
        log.info("SCENARIO: Multiple message test");

        producer.send(new ProducerRecord<>(INPUT_TOPIC, 0, "0", "2"));
        producer.send(new ProducerRecord<>(INPUT_TOPIC, 0, "0", "3"));
        producer.send(new ProducerRecord<>(INPUT_TOPIC, 1, "1", "4"));
        producer.flush();

        var records = consumer.pollNext(3, false, QUICK_TAKE_TIMEOUT);
        var expected = Set.of("The 2 prime number is 3", "The 3 prime number is 5", "The 4 prime number is 7");
        var found = records.stream()
                .map(ConsumerRecord::value)
                .collect(Collectors.toSet());

        if (!expected.equals(found)) {
            log.info("""
                    FAIL: The messages were not what we expected. Expected:
                    
                    {}
                    
                    But found:
                    
                    {}""", expected, found);
        } else {
            log.info("SUCCESS: Found the messages we expected");
        }
    }

    void load() {
        log.info("Simulating a load of work. Producing 30 messages to the input Kafka topic. Each messages requests to compute the 1 millionth prime number");

        for (int i = 0; i < 30; i++) {
            int partition = i % 2;
            int key = i % 10;
            var record = new ProducerRecord<>(INPUT_TOPIC, partition, String.valueOf(key), String.valueOf(1_000_000));
            producer.send(record);
        }

        producer.flush();
        consumer.pollNext(30, true, Duration.ofSeconds(30));
    }

    void loadBatchy() {
        log.info("Simulating batchy work: two loads separated by time.");

        for (int i = 0; i < 10; i++) {
            var record = new ProducerRecord<>(INPUT_TOPIC, 0, "0", String.valueOf(1_000_000));
            producer.send(record);
        }
        producer.flush();

        // A few seconds later, another batch of messages come in. These messages are on a different partition than the
        // first batch, so ideally, the consumer should be able to handle them right away.
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < 10; i++) {
                var record = new ProducerRecord<>(INPUT_TOPIC, 1, "1", String.valueOf(1_000_000));
                producer.send(record);
            }
            producer.flush();
        });

        consumer.pollNext(20, true, Duration.ofSeconds(30));
    }
}

/**
 * A simple Kafka consumer abstraction tailored for the test harness. It's extension point is the 'recordProcessor'
 * function.
 */
class TestHarnessConsumer {

    private static final Logger log = LoggerFactory.getLogger("test-harness");

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

    List<ConsumerRecord<String, String>> pollNext(int count, boolean report, Duration timeout) {
        log.debug("Polling for %,d records ...".formatted(count));
        if (!active.compareAndSet(false, true)) {
            throw new IllegalStateException("There is already an active 'pollNext' request. It is an error to start another one.");
        }

        var session = new PollNextSession(count, report);
        try {
            return session.future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Timed out waiting for %,d records in the 'pollNext' request".formatted(count), e);
        }
    }

    void stop() {
        log.debug("Stopping...");
        active.getAndSet(false);
        consumer.wakeup();
        consumer.close();
    }
}
