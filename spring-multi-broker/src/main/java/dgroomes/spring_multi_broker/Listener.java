package dgroomes.spring_multi_broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Listener {

    private static final Logger log = LoggerFactory.getLogger(Listener.class);

    /**
     * Listen to messages on the topic in Kafka broker "A".
     * <p>
     * The special trick here is the "properties" field in the KafkaListener annotation. There are many classes, interfaces
     * and configuration hooks within the Spring Boot and Spring Kafka software machinery but this particular trick is the
     * perfect fit for configuring the app to consume from two different Kafka brokers. See the "listenBrokerB" method
     * which uses a different value for the "bootstrap.servers" property.
     */
    @KafkaListener(topics = "my-messages", properties = {"bootstrap.servers=${app.bootstrap-servers-broker-a}"})
    public void listenBrokerA(String message) {
        log.info("Got Kafka message on the 'A' broker: {}", message);
    }

    /**
     * Listen to messages on the topic in Kafka broker "B"
     */
    @KafkaListener(topics = "my-messages", properties = {"bootstrap.servers=${app.bootstrap-servers-broker-b}"})
    public void listenBrokerB(String message) {
        log.info("Got Kafka message on the 'B' broker: {}", message);
    }
}
