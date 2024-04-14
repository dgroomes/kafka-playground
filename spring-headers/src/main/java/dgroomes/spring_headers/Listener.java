package dgroomes.spring_headers;

import dgroomes.spring_headers.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class Listener {

    private static final Logger log = LoggerFactory.getLogger(Listener.class);

    /**
     * Listen for messages from the "my-messages" Kafka topic.
     * <p>
     * Also, get the metadata and "__TypeId__" Kafka header from each message. That's a pretty cool declarative feature!
     * That's useful for getting partition-offset which we often want for logging errors.
     */
    @KafkaListener(topics = "my-messages")
    public void listen(
            Message message,
            ConsumerRecordMetadata metaData,
            @Header("__TypeId__") String typeId) {

        log.info("Got Kafka message (metadata|__TypeId__|message): {}|{}|{}", metaData, typeId, message);
    }
}
