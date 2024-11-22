package dgroomes.runner;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A test harness for exercising the "kafka-consumer-algorithms" application via different scenarios and load simulations. This
 * program sends Kafka messages to the input topic ('input') and may read messages from the output topic ('output')
 * to verify correctness.
 */
public class TestHarness implements AutoCloseable {

    Logger log = LoggerFactory.getLogger("runner");
    static String BROKER_HOST = "localhost:9092";
    static String INPUT_TOPIC = "input";
    static String OUTPUT_TOPIC = "output";
    static Duration POLL_TIMEOUT = Duration.ofMillis(250);
    static Duration REPORTING_DELAY = Duration.ofSeconds(2);

    TestHarnessConsumer consumer;
    KafkaProducer<String, String> producer;

    void setup() {
        consumer = new TestHarnessConsumer();
        consumer.seekToEnd();

        // Initialize the producer
        var props = Map.<String, Object>of("bootstrap.servers", BROKER_HOST,
                "key.serializer", "org.apache.kafka.common.serialization.StringSerializer",
                "value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(props);
    }

    @Override
    public void close() {
        Optional.ofNullable(consumer).ifPresent(TestHarnessConsumer::stop);
    }

    public enum Scenario {
        ONE_MESSAGE,
        MULTI_MESSAGE,
        LOAD_BATCH,
        LOAD_BATCH_UNEVEN,
        LOAD_STEADY
    }

    void run(Scenario scenario) {
        setup();

        Runnable fn = switch (scenario) {
            case ONE_MESSAGE -> this::oneMessage;
            case MULTI_MESSAGE -> this::multiMessage;
            case LOAD_BATCH -> this::loadBatch;
            case LOAD_BATCH_UNEVEN -> this::loadBatchUneven;
            case LOAD_STEADY -> this::loadSteady;
        };

        fn.run();
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

        var foundAll = consumer.pollNext(1, false);
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

        var records = consumer.pollNext(3, false);
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

    void loadBatch() {
        log.info("Simulating a 'batch' of work: a shot of messages all at once");

        for (int i = 0; i < 20; i++) {
            int partition = i % 2;
            int key = i % 3;
            var record = new ProducerRecord<>(INPUT_TOPIC, partition, String.valueOf(key), String.valueOf(1_000_000));
            producer.send(record);
        }

        producer.flush();
        consumer.pollNext(20, true);
    }

    void loadBatchUneven() {
        log.info("Simulating 'uneven' work: one batch of messages comes and then a second batch comes shortly thereafter.");

        for (int i = 0; i < 10; i++) {
            int key = i % 3;
            var record = new ProducerRecord<>(INPUT_TOPIC, 0, String.valueOf(key), String.valueOf(1_000_000));
            producer.send(record);
        }
        producer.flush();

        // A few seconds later, another batch of messages come in. These messages are on a different partition than the
        // first batch, so ideally, a consumer should be able to handle them right away. However, consumers that
        // sequentially process the whole poll group before moving on to the next will be bottle-necked.
        spawn(() -> {
            Thread.sleep(3_000);

            for (int i = 0; i < 10; i++) {
                int key = i % 3;
                var record = new ProducerRecord<>(INPUT_TOPIC, 1, String.valueOf(key), String.valueOf(1_000_000));
                producer.send(record);
            }
            producer.flush();
        });

        consumer.pollNext(20, true);
    }

    void loadSteady() {
        log.info("Simulating a steady stream of work");

        spawn(() -> {
            for (int i = 0; i < 20; i++) {
                int partition = i % 2;
                int key = i % 3;
                var record = new ProducerRecord<>(INPUT_TOPIC, partition, String.valueOf(key), String.valueOf(1_000_000));
                producer.send(record);
                Thread.sleep(333);
            }
        });

        consumer.pollNext(20, true);
    }

    private static void spawn(ThrowingRunnable runnable) {
        Thread.ofVirtual().start(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}

interface ThrowingRunnable {
    void run() throws Exception;
}

