package dgroomes;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TheTest extends BaseTest {

    /**
     * This is a sanity check test case.
     *
     * Send a unique message and assert that it shows up on the output Kafka topic.
     *
     * It's useful to send a message with a unique body because it eliminates the chances of the program, or you the
     * developer, of crossing your wires and seeing an old message but mistaking it for a new one. In Kafka, we often
     * see the same messages over and over because we might play back the whole topic or a consumer failed to commit
     * its offsets and consumes the same message repeatedly. A good way to generate a unique message is to use the
     * current time.
     */
    @Test
    @Order(1)
    void identity() throws Exception {
        // Arrange
        var time = LocalTime.now();
        var uniqueMsg = String.format("The current time is: %s", time);

        // Act
        send(uniqueMsg);

        // Assert
        var record = consumer.take();
        var expected = String.format("%s%s%s", '"', uniqueMsg, '"');
        assertThat(record.value()).isEqualTo(expected);
    }

    /**
     * This is a 'happy path' test case.
     * <p>
     * Exercise the 'app' by sending a message to the input Kafka topic, waiting a brief time for the 'app' to process
     * the message, and then consume from the output topic with the expectation that the 'app' successfully processed
     * and published a quoted version of the input message. Assert te contents of the output message.
     */
    @Test
    void quote() throws Exception {
        // Arrange
        var msg = "Did you say \"hello\"?";

        // Act
        send(msg);

        // Assert
        var record = consumer.take();
        assertThat(record.value()).isEqualTo("\"Did you say \\\"hello\\\"?\"");
    }
}
