package dgroomes.spring_seekable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Listens to incoming messages from the Kafka topic "my-messages"
 */
@Component
public class Listener extends SeekableKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(Listener.class);

    private final AtomicInteger consumedMessages = new AtomicInteger(0);

    @KafkaListener(topics = "my-messages")
    public void listen(String message, Acknowledgment acknowledgment) {
        log.info("Got Kafka message: {}", message);
        consumedMessages.incrementAndGet();
        acknowledgment.acknowledge();
    }

    /**
     * @return the number of consumed messages
     */
    public int consumedMessages() {
        return consumedMessages.get();
    }
}
