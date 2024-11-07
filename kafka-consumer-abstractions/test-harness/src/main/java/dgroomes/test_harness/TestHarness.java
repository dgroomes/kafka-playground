package dgroomes.test_harness;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * A test harness for exercising the "kafka-consumer-abstractions" application via different scenarios and load simulations. This
 * program sends Kafka messages to the input topic ('input') and may read messages from the output topic ('output')
 * to verify correctness.
 */
public class TestHarness {

    String[] args;
    Logger log = LoggerFactory.getLogger("test-harness");
    String BROKER_HOST = "localhost:9092";
    String INPUT_TOPIC = "input";
    String OUTPUT_TOPIC = "output";
    Duration POLL_TIMEOUT = Duration.ofMillis(250);
    Duration TAKE_TIMEOUT = Duration.ofSeconds(5);

    AtomicBoolean go = new AtomicBoolean(true);
    KafkaConsumer<String, String> consumer;
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

        this.consumer = new KafkaConsumer<>(Map.of("bootstrap.servers", BROKER_HOST,
                "key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer",
                "value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));

        // Assign the consumer to the topic partitions and seek to the end of those topic partitions.
        {
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
            case "load-heavy-key" -> loadHeavyKey();
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

        var foundOpt = pollNext();
        if (foundOpt.isPresent()) {
            var found = foundOpt.get();
            if (!found.key().equals(key)) {
                log.error("Fail: The key was not what we expected. Expected: {}, but found: {}", key, found.key());
            }

            var expected = "The 2 prime number is 3";

            if (!expected.equals(found.value())) {
                log.error("""
                        FAIL: The message was not what we expected. Expected:
                        
                        {}
                        
                        But found:
                        
                        {}""", expected, found.value());
            } else {
                log.info("SUCCESS: Found the message we expected");
            }
        }
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

        List<ConsumerRecord<String, String>> records = pollAmount(3);

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

    Optional<ConsumerRecord<String, String>> pollNext() {
        var expiration = Instant.now().plus(TAKE_TIMEOUT);

        while (true) {
            if (Instant.now().isAfter(expiration)) {
                log.error("Fail: Timed out waiting to receive a message.");
                return Optional.empty();
            }

            if (!go.get()) {
                log.debug("Breaking from the polling loop.");
                return Optional.empty();
            }

            var records = consumer.poll(POLL_TIMEOUT);
            if (records.isEmpty()) {
                log.debug("Poll yielded empty record set.");
                continue;
            } else if (records.count() > 1) {
                log.error("Fail: Expected at most one record but found more than one");
                return Optional.empty();
            }

            var record = records.iterator().next();
            log.debug("Received a record: {}", record);
            return Optional.of(record);
        }
    }

    @SuppressWarnings("SameParameterValue")
    List<ConsumerRecord<String, String>> pollAmount(int amount) {
        var expiration = Instant.now().plus(TAKE_TIMEOUT);

        var records = new ArrayList<ConsumerRecord<String, String>>();
        while (true) {
            if (records.size() == amount) {
                return records;
            }

            if (records.size() > amount) {
                throw new IllegalStateException("Fail: Expected at most " + amount + " records but found more than that");
            }

            if (Instant.now().isAfter(expiration)) {
                throw new IllegalStateException("Fail: Timed out waiting to receive a message.");
            }

            if (!go.get()) {
                log.debug("Breaking from the polling loop.");
                return records;
            }

            var _records = consumer.poll(POLL_TIMEOUT);
            if (_records.isEmpty()) {
                log.debug("Poll yielded empty record set.");
                continue;
            }

            _records.iterator().forEachRemaining(records::add);
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
    }

    void loadHeavyKey() {
        log.info("Simulating a load of work where one partition-key has an outsized amount of the workload.");

        for (int i = 0; i < 10; i++) {
            // All messages go to the same partition and using the same key. This means we'll get no parallelism.
            var record = new ProducerRecord<>(INPUT_TOPIC, 0, "0", String.valueOf(1_000_000));
            producer.send(record);
        }

        producer.flush();
    }
}
