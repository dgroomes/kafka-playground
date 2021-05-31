package dgroomes;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This base class provides the boilerplate and helper functionality related to producing to and consuming from the local
 * Kafka cluster.
 */
public class BaseTest {

    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    private static final String INPUT_TOPIC = "input-text";
    private static final String OUTPUT_TOPIC = "quoted-text";
    private static final int TIMEOUT = 200;

    protected Producer<Void, String> producer;
    protected SynchronousKafkaConsumer consumer;

    /**
     * Initialize the Kafka consumer and producer objects.
     */
    @BeforeEach
    public void setup() {
        Properties kafkaConfig = new Properties();
        kafkaConfig.putAll(Map.of(
                "bootstrap.servers", "localhost:9092",
                "key.serializer", "org.apache.kafka.common.serialization.StringSerializer",
                "value.serializer", "org.apache.kafka.common.serialization.StringSerializer",
                "key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer",
                "value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));

        this.consumer = new SynchronousKafkaConsumer(kafkaConfig, OUTPUT_TOPIC, TIMEOUT);
        this.producer = new KafkaProducer<>(kafkaConfig);
    }

    @AfterEach
    public void cleanUp() {
        consumer.close();
        producer.close();
    }

    /**
     * (Synchronous) Send a test message to the input Kafka topic. This will block until the message is sent.
     *
     * @param msg the message to send to Kafka
     */
    protected void send(String msg) throws ExecutionException, InterruptedException {
        log.debug("Sending message: {}", msg);
        ProducerRecord<Void, String> record = new ProducerRecord<>(INPUT_TOPIC, msg);
        Future<RecordMetadata> future = producer.send(record);
        future.get();
    }
}
