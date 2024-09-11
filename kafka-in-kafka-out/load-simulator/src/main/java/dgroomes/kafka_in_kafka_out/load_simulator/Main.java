package dgroomes.kafka_in_kafka_out.load_simulator;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Simulate load by generating many Kafka messages. See the README for more info.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final String KAFKA_BROKER_HOST = "localhost:9092";
    private static final String KAFKA_TOPIC = "input-text";

    private final KafkaProducer<Void, String> producer;

    public Main(KafkaProducer<Void, String> kafkaProducer) {
        this.producer = kafkaProducer;
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException(String.format("Expected exactly 1 argument but found %s", args.length));
        }

        int numMessages = Integer.parseInt(args[0]);
        int numbersPerMsg = Integer.parseInt(args[1]);
        int sortFactor = Integer.parseInt(args[2]);

        log.info("Simulating %,d generated messages, with %,d numbers per message, a sort factor of %,d, and producing them to the Kafka topic '%s'".formatted(numMessages, numbersPerMsg, sortFactor, KAFKA_TOPIC));

        var main = new Main(kafkaProducer());
        main.generateLoad(numMessages, numbersPerMsg, sortFactor);

        log.info("Done. %,d messages produced to and acknowledged by the Kafka broker.".formatted(numMessages));
    }

    /**
     * Generate and produce many Kafka messages. Periodically flush the producer so it does not accumulate too many messages
     * in memory without flushing.
     * <p>
     *
     * @param numberOfMessages  the number of messages to generate and produce
     * @param numbersPerMessage the number of random numbers to include in each message
     */
    private void generateLoad(int numberOfMessages, int numbersPerMessage, int sortFactor) {
        var random = new Random(0);
        var sortFactorBytes = String.valueOf(sortFactor).getBytes();

        for (int i = 0; i < numberOfMessages; i++) {
            var msg = IntStream.generate(() -> random.nextInt(100))
                    .limit(numbersPerMessage)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(" "));

            var record = new ProducerRecord<Void, String>(KAFKA_TOPIC, null, msg);
            record.headers().add(new RecordHeader("sort_factor", sortFactorBytes));

            producer.send(record);
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
        return new KafkaProducer<>(props);
    }
}
