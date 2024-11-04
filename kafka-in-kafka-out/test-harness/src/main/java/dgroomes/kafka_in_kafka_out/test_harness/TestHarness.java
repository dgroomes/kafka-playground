package dgroomes.kafka_in_kafka_out.test_harness;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A test harness for exercising the "kafka-in-kafka-out" application via different scenarios and load simulations. This
 * program sends Kafka messages to the input topic ('input-text') and may read messages from the output topic ('lowest-word')
 * to verify correctness.
 */
public class TestHarness {

    String[] args;
    Logger log = LoggerFactory.getLogger("test-harness");
    String BROKER_HOST = "localhost:9092";
    String INPUT_TOPIC = "input-text";
    String OUTPUT_TOPIC = "lowest-word";
    Duration POLL_TIMEOUT = Duration.ofMillis(250);
    Duration TAKE_TIMEOUT = Duration.ofSeconds(5);

    AtomicBoolean go = new AtomicBoolean(true);
    KafkaConsumer<Integer, String> consumer;
    KafkaProducer<Integer, String> producer;

    public TestHarness(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) throws Exception {
        new TestHarness(args).run();
    }

    void run() throws Exception {
        if (args.length == 0) {
            log.error("No arguments found. You must specify a test/simulation scenario: 'one-message', 'multi-message', or 'load'");
            System.exit(1);
        }

        this.consumer = new KafkaConsumer<>(Map.of("bootstrap.servers", BROKER_HOST,
                "key.deserializer", "org.apache.kafka.common.serialization.IntegerDeserializer",
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
                    "key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer",
                    "value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            producer = new KafkaProducer<>(props);
        }

        var scenario = args[0];
        switch (scenario) {
            case "one-message" -> oneMessage();
            case "multi-message" -> multiMessage();
            case "load-cpu-intensive" -> loadCpuIntensive();
            default -> {
                log.error("Unknown scenario: '{}'", scenario);
                System.exit(1);
            }
        }
    }

    /**
     * Send a message to the input topic and then poll for the message on the "lowest word" topic. The
     * transformed message should be the time string (e.g. "10:15:30") from the input message.
     */
    void oneMessage() throws ExecutionException, InterruptedException {
        log.info("SCENARIO: Single message test");
        var now = LocalTime.now();
        var uniqueMsg = String.format("current time: %s", now);

        var future = producer.send(new ProducerRecord<>(INPUT_TOPIC, 1, uniqueMsg));
        future.get();
        // The output topic contains the lowest alphanumeric word from the input message. The 'now' string will
        // always start with a number, like "10:15:30". It will always be the lowest alphanumeric word because it's
        // the only number in our test message.
        var expected = now.toString();
        var foundOpt = pollNext();
        if (foundOpt.isPresent()) {
            var found = foundOpt.get();
            if (found.key() != 1) {
                log.error("Fail: The key was not what we expected. Expected: 1, but found: {}", found.key());
            }
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
    void multiMessage() throws ExecutionException, InterruptedException {
        log.info("SCENARIO: Multiple message test");

        // Let's imagine that partition 0 takes even-numbered keys and partition 1 takes odd-numbered keys.
        //
        // Send multiple messages of the same key to partition 0.
        producer.send(new ProducerRecord<>(INPUT_TOPIC, 0, 0, "hello there"));
        producer.send(new ProducerRecord<>(INPUT_TOPIC, 0, 0, "what's up"));
        // Send a message to partition 1
        var f = producer.send(new ProducerRecord<>(INPUT_TOPIC, 1, 1, "the sky is blue"));
        f.get();

        List<ConsumerRecord<Integer, String>> records = pollAmount(3);

        var expected = Set.of("hello", "up", "blue");
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

    Optional<ConsumerRecord<Integer, String>> pollNext() {
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
    List<ConsumerRecord<Integer, String>> pollAmount(int amount) {
        var expiration = Instant.now().plus(TAKE_TIMEOUT);

        var records = new ArrayList<ConsumerRecord<Integer, String>>();
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

    /**
     * Generate and produce many Kafka messages that will be CPU-intensive to process.
     */
    void loadCpuIntensive() {
        args = Arrays.copyOfRange(args, 1, args.length);
        if (args.length != 3) {
            throw new IllegalArgumentException(String.format("The 'load' scenario expects exactly 3 positional argument but found %s", args.length));
        }

        int numMessages = Integer.parseInt(args[0]);
        int numbersPerMsg = Integer.parseInt(args[1]);
        int sortFactor = Integer.parseInt(args[2]);

        log.info("Simulating %,d generated messages, with %,d numbers per message, a sort factor of %,d, and producing them to the Kafka topic '%s'".formatted(numMessages, numbersPerMsg, sortFactor, INPUT_TOPIC));

        var random = new Random(0);
        var sortFactorBytes = String.valueOf(sortFactor).getBytes();

        for (int i = 0; i < numMessages; i++) {
            var msg = IntStream.generate(() -> random.nextInt(100))
                    .limit(numbersPerMsg)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(" "));

            var key = random.nextInt(100);
            var record = new ProducerRecord<>(INPUT_TOPIC, key, msg);
            record.headers().add(new RecordHeader("sort_factor", sortFactorBytes));

            producer.send(record);
        }

        producer.flush();

        log.info("Done. %,d messages produced to and acknowledged by the Kafka broker.".formatted(numMessages));
    }
}
