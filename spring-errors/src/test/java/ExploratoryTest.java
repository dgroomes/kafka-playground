import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dgroomes.spring_errors.model.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExploratoryTest {

    public static void main(String[] args) throws JsonProcessingException {
        new ExploratoryTest().doit();
    }

    @Test
    void doit() throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        var json = """
                {
                  "message": "hello(55)",
                  "time": "1"
                }
                """;

        var obj = objectMapper.readValue(json, Message.class);

        assertEquals(1, obj.time);
    }
}
