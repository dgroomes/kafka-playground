package dgroomes.spring_errors.model;

import java.util.Objects;

public class Message {

    public String message;
    public int time;

    @Override
    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
                ", time=" + time +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message1 = (Message) o;
        return time == message1.time &&
                Objects.equals(message, message1.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, time);
    }
}
