package dgroomes.springbarebones;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple Java program to process messages from a Kafka topic using abstractions from the Spring for Apache Kafka
 * library.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static final String TOPIC = "my-messages";

    public static void main(String[] args) {
        log.info("Listening for Kafka messages using Spring for Apache Kafka's very useful abstractions");

        var container = kafkaMessageListenerContainer();
        container.start();
    }

    /**
     * KafkaMessageListenerContainer is the kernel of the "Spring for Apache Kafka" library (at least from my vantage
     * point). It is an effective abstraction over the Java Kafka client library.
     */
    public static KafkaMessageListenerContainer<Void, String> kafkaMessageListenerContainer() {
        ContainerProperties containerProperties = new ContainerProperties(TOPIC);
        containerProperties.setMessageListener(new Listener());

        var consumerFactory = new DefaultKafkaConsumerFactory<Void, String>(consumerConfigs());
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    public static Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "spring-barebones-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    public static class Listener implements MessageListener<Void, String> {

        private static final Logger log = LoggerFactory.getLogger(Listener.class);

        @Override
        public void onMessage(ConsumerRecord<Void, String> record) {
            log.info("Got message: {}", record.value());
        }
    }
}
