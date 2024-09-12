package dgroomes.kafka_in_kafka_out.test_harness;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.lang.System.out;

/**
 * A single-threaded program that tests the "kafka-in-kafka-out" application by sending a message and verifying that
 * a quoted version of that message was created.
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

    public static void main(String[] args) throws Exception {
        new TestHarness().run();
    }

    void run() throws Exception {
        Signal.handle(new Signal("INT"), signal -> {
            LOG.debug("Interrupt signal received.");
            go.set(false);
        });

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

        // Run the test. Send a message to the input topic and then poll for the message on the output topic. The
        // received message should be quoted.
        {
            var now = LocalTime.now();
            var uniqueMsg = String.format("current time: %s", now);

            var props = Map.<String, Object>of("bootstrap.servers", BROKER_HOST,
                    "key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer",
                    "value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            try (var producer = new KafkaProducer<Integer, String>(props)) {

                var record1 = new ProducerRecord<>(INPUT_TOPIC, 1, uniqueMsg);
                var future = producer.send(record1);
                future.get();
            }

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
                            Fail: The message was not what we expected. Expected:
                            
                            %s
                            
                            But found:
                            
                            %s%n""", expected, found.value());
                } else {
                    out.println("Success: Found the message we expected");
                }
            }

            consumer.close();
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
}