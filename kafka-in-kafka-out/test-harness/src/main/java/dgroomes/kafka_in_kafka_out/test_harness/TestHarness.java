package dgroomes.kafka_in_kafka_out.test_harness;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.lang.System.out;

/**
 * A single-threaded program that tests the "kafka-in-kafka-out" application by sending messages and verifying that
 * the "lowest word" transformations of those messages were created on the output topic.
 */
public class TestHarness {

    Logger LOG = LoggerFactory.getLogger("main");
    String BROKER_HOST = "localhost:9092";
    String INPUT_TOPIC = "input-text";
    String OUTPUT_TOPIC = "lowest-word";
    Duration POLL_TIMEOUT = Duration.ofMillis(250);
    Duration TAKE_TIMEOUT = Duration.ofSeconds(5);

    AtomicBoolean go = new AtomicBoolean(true);
    KafkaConsumer<Integer, String> consumer;
    KafkaProducer<Integer, String> producer;

    public static void main(String[] args) throws Exception {
        new TestHarness().run();
    }

    void run() throws Exception {
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
                LOG.debug("Partition: {}, Offset: {}", partition, position);
            });
        }

        // Initialize the producer
        {
            var props = Map.<String, Object>of("bootstrap.servers", BROKER_HOST,
                    "key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer",
                    "value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            producer = new KafkaProducer<>(props);
        }

        singleMessageTest();
        multipleMessageTest();
    }

    /**
     * Send a message to the input topic and then poll for the message on the "lowest word" topic. The
     * transformed message should be the time string (e.g. "10:15:30") from the input message.
     */
    void singleMessageTest() throws ExecutionException, InterruptedException {
        out.println("'Single message test' ...");
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
                out.printf("Fail: The key was not what we expected. Expected: 1, but found: %s%n", found.key());
            }
            if (!expected.equals(found.value())) {
                out.printf("""
                        FAIL: The message was not what we expected. Expected:
                        
                        %s
                        
                        But found:
                        
                        %s%n""", expected, found.value());
            } else {
                out.println("SUCCESS: Found the message we expected");
            }
        }
    }

    /**
     * Send multiple messages of the same key, and messages to multiple partitions.
     */
    void multipleMessageTest() throws ExecutionException, InterruptedException {
        out.println("'Multiple message test' ...");

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
            out.printf("""
                    FAIL: The messages were not what we expected. Expected:
                    
                    %s
                    
                    But found:
                    
                    %s%n""", expected, found);
        } else {
            out.println("SUCCESS: Found the messages we expected");
        }
    }

    Optional<ConsumerRecord<Integer, String>> pollNext() {
        var expiration = Instant.now().plus(TAKE_TIMEOUT);

        while (true) {
            if (Instant.now().isAfter(expiration)) {
                out.println("Fail: Timed out waiting to receive a message.");
                return Optional.empty();
            }

            if (!go.get()) {
                LOG.debug("Breaking from the polling loop.");
                return Optional.empty();
            }

            var records = consumer.poll(POLL_TIMEOUT);
            if (records.isEmpty()) {
                LOG.debug("Poll yielded empty record set.");
                continue;
            } else if (records.count() > 1) {
                out.println("Fail: Expected at most one record but found more than one");
                return Optional.empty();
            }

            var record = records.iterator().next();
            LOG.debug("Received a record: {}", record);
            return Optional.of(record);
        }
    }

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
                LOG.debug("Breaking from the polling loop.");
                return records;
            }

            var _records = consumer.poll(POLL_TIMEOUT);
            if (_records.isEmpty()) {
                LOG.debug("Poll yielded empty record set.");
                continue;
            }

            _records.iterator().forEachRemaining(records::add);
        }
    }

}
