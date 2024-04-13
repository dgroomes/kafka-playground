package dgroomes.kafka_in_kafka_out.load_simulator;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.Objects;
import java.util.Properties;

/**
 * Simulate load by generating many Kafka messages. See the README for more info.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final String KAFKA_BROKER_HOST = "localhost:9092";
    private static final String KAFKA_TOPIC = "input-text";
    private static final int NUMBER_OF_MESSAGES_BETWEEN_FLUSHES = 100_000;
    private static final String PRODUCER_COMPRESSION = System.getenv("PRODUCER_COMPRESSION");

    private final KafkaProducer<Void, String> producer;

    public Main(KafkaProducer<Void, String> kafkaProducer) {
        this.producer = kafkaProducer;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException(String.format("Expected exactly 1 argument but found %s", args.length));
        }

        int numberOfMessages = Integer.parseInt(args[0].replace("_", ""));
        String numberOfMessagesHumanReadable = NumberFormat.getInstance().format(numberOfMessages);
        log.info("Simulating {} generated JSON messages and producing them to to the Kafka topic '{}'", numberOfMessagesHumanReadable, KAFKA_TOPIC);

        var main = new Main(kafkaProducer());
        main.generateLoad(numberOfMessages);

        log.info("Done. {} messages produced to and acknowledged by the Kafka broker.", numberOfMessagesHumanReadable);
    }

    private static final String MESSAGE_FORMAT = "{\n" +
            "  \"message\": \"hello %s\"\n" +
            "}";

    /**
     * Generate and produce many Kafka messages. Periodically flush the producer so it does not accumulate too many messages
     * in memory without flushing.
     * <p>
     * The messages will be in the JSON format which means that they should compress effectively because of the high
     * amount of duplicate (and thus easily compressed) character patterns between messages.
     *
     * @param numberOfMessages the number of messages to generate and produce
     */
    private void generateLoad(int numberOfMessages) {
        for (int i = 0; i < numberOfMessages; i++) {
            var record = new ProducerRecord<Void, String>(KAFKA_TOPIC, null, String.format(MESSAGE_FORMAT, i));
            producer.send(record);

            if (i % NUMBER_OF_MESSAGES_BETWEEN_FLUSHES == 0) {
                log.debug("Flushing at {} messages", i);
                producer.flush();
                log.debug("Flushed.");
            }
        }

        producer.flush();
    }

    /**
     * Construct a KafkaProducer
     */
    public static KafkaProducer<Void, String> kafkaProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", KAFKA_BROKER_HOST);
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        String compression = Objects.requireNonNullElse(PRODUCER_COMPRESSION, "none");
        log.info("Kafka producer compression: '{}'", compression);
        props.put("compression.type", compression);

        return new KafkaProducer<>(props);
    }
}
