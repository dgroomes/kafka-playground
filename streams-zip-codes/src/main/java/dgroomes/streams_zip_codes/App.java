package dgroomes.streams_zip_codes;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * A Kafka Streams app
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private final KafkaStreams kafkaStreams;

    public App() {
        final Properties props = config();
        Topology topology = new TopologyBuilder().build();
        log.info("Created a Kafka Streams topology:\n\n{}", topology.describe());

        kafkaStreams = new KafkaStreams(topology, props);
    }

    public static Properties config() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-zip-codes");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 0);

        @SuppressWarnings("resource") var stringSerdesName = Serdes.String().getClass().getName();
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, stringSerdesName);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, stringSerdesName);

        props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 10);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return props;
    }

    /**
     * Start the Kafka Streams topology
     */
    void start() {
        kafkaStreams.start();
    }

    /**
     * Stop the Kafka Streams topology.
     */
    void stop() {
        kafkaStreams.close();
    }
}
