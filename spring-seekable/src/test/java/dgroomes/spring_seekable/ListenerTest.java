package dgroomes.spring_seekable;

import dgroomes.spring_seekable.Listener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.template.default-topic=my-messages"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class ListenerTest {

    @Autowired
    KafkaTemplate<?, String> kafkaTemplate;

    @Autowired
    Listener listener;

    /**
     * Send a test message and assert that it is consumed by the app
     */
    @Test
    @Timeout(10)
    void sendAndReceive() throws ExecutionException, InterruptedException {
        var consumedMessagesBefore = listener.consumedMessages();
        long now = Instant.now().getEpochSecond();
        String msg = String.format("hello: %d", now);

        kafkaTemplate.sendDefault(msg).get();
        Thread.sleep(1000);

        int consumedMessagesAfter = listener.consumedMessages();
        assertThat(consumedMessagesAfter).isEqualTo(consumedMessagesBefore + 1);
    }
}
