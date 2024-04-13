package dgroomes.kafka_playground.streams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.StreamPartitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

/**
 * A toy implementation of a Kafka Streams app. Designed for educational purposes!
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    public static final String INPUT_TOPIC = "plaintext-input";
    public static final String STREAMS_INPUT_TOPIC = "streams-plaintext-input";
    public static final String STREAMS_OUTPUT_TOPIC = "streams-wordcount-output";
    public static final int INPUT_MESSAGE_SLEEP = 1000;

    private final KafkaStreams kafkaStreams;

    public App() {
        Properties props = config();
        Topology topology = topology();
        log.info("Created a Kafka Streams topology:\n\n{}", topology.describe());

        kafkaStreams = new KafkaStreams(topology, props);
    }

    public static Properties config() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-wordcount");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        // I don't know why I set this to 0. And 'statestore.cache.max.bytes' is actually the replacement config for the
        // now deprecated 'cache.max.bytes.buffering' config. Annoyingly, the new config is not documented on the Kafka
        // developer guide page, and the old config is still documented and not actually marked as deprecated. https://kafka.apache.org/37/documentation/streams/developer-guide/config-streams.html
        props.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 0);

        @SuppressWarnings("resource") String stringSerdesName = Serdes.String().getClass().getName();

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, stringSerdesName);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, stringSerdesName);
        props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 10);

        // Setting offset reset to the earliest so that we can re-run the demo code with the same pre-loaded data
        // Note: To re-run the demo, you need to use the offset reset tool:
        // https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Streams+Application+Reset+Tool
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    /**
     * Build a streams topology
     */
    static Topology topology() {
        var builder = new StreamsBuilder();
        KStream<String, String> source = builder.stream(INPUT_TOPIC);

        /*
        Pass the plaintext-input topic data into a "streams-plaintext-input" topic which is configured with
        a higher number of partitions, so we can get more concurrency out of the topology. We want the
        "streams-plaintext-input" to get messages produced to it with an even distribution across its
        partitions, so we hash on the value to get an approximate even distribution for partition assignment.

        Note that the "through" method is deprecated in favor of the "repartition" method, but the "repartition"
        method auto-creates a topic. We are not interested in letting the streams application create topics for
        us. This is the same energy as letting your application auto-create SQL tables. Fine for local dev but
        doesn't make much sense to me in a production context. Think about the operational lifecycle. What happens when
        you need to clean out old data because you are running out of disk space? If you didn't understand the topics
        when they were auto-created, why would you know be confident in knowing which ones are safe to delete?
        */
        @SuppressWarnings("deprecation") KStream<String, String> repartitioned = source.through(STREAMS_INPUT_TOPIC, Produced.streamPartitioner(new StreamPartitioner<String, String>() {
            @Override
            public Integer partition(String topic, String key, String value, int numPartitions) {
                var bytes = value.getBytes();
                // Similar to https://github.com/apache/kafka/blob/c6adcca95f03758089715c60e806a8090f5422d9/clients/src/main/java/org/apache/kafka/clients/producer/internals/DefaultPartitioner.java#L71
                return Utils.toPositive(Utils.murmur2(bytes)) % numPartitions;
            }
        }));

        KTable<String, Long> counts = repartitioned
                .mapValues(value -> {
                    try {
                        log.info("Input message received: {}. Sleeping for {}ms", value, INPUT_MESSAGE_SLEEP);
                        Thread.sleep(INPUT_MESSAGE_SLEEP);
                    } catch (InterruptedException e) {
                        log.error("Interrupted while sleeping", e);
                    }
                    return value;
                })
                .flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split(" ")))
                .groupBy((key, value) -> value)
                .count();

        // need to override value serde to Long type
        counts.toStream().to(STREAMS_OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
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
