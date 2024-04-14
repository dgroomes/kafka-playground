package dgroomes.spring_headers.model;

import java.util.Objects;

/**
 * Concrete implementation of the Message class. Illustrating an interesting feature in Spring Kafka where we can listen
 * for messages of an interface type and rely on the Spring Kafka machinery to detect the actual concrete type via the
 * the "__TypeId__" Kafka header. Read about this feature at https://docs.spring.io/spring-kafka/docs/2.5.1.RELEASE/reference/html/#payload-conversion-with-batch
 */
public class MessageA implements Message {

    public String message;
    public String type;
    public String a;

    @Override
    public String message() {
        return message;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public String toString() {
        return "MessageA{" +
                "message='" + message + '\'' +
                ", type='" + type + '\'' +
                ", a='" + a + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageA messageA = (MessageA) o;
        return Objects.equals(message, messageA.message) &&
                Objects.equals(type, messageA.type) &&
                Objects.equals(a, messageA.a);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, type, a);
    }
}
