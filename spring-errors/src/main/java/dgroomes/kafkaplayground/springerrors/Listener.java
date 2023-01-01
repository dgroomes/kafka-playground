package dgroomes.kafkaplayground.springerrors;

import dgroomes.kafkaplayground.springerrors.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Listener {

    private static final Logger log = LoggerFactory.getLogger(Listener.class);

    /**
     * Listen for messages from the "my-messages" Kafka topic.
     */
    @KafkaListener(topics = "my-messages")
    public void listen(Message message) {
        log.info("Got Kafka message: {}", message);
    }
}
