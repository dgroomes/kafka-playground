package dgroomes.streams_zip_codes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

/**
 * A Jackson based JSON Kafka serde.
 *
 * @param <T> the serde will deserialize JSON documents to this type and serialize Java objects from this type to JSON.
 */
public class JsonSerde<T> implements Serde<T> {

    private final Serializer<T> serializer;
    private final Deserializer<T> deserializer;

    public JsonSerde(TypeReference<T> type) {
        var objectMapper = new ObjectMapper().registerModule(new ParameterNamesModule());
        // Don't fail on unknown properties. This is more conventional default behavior.
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.serializer = (topic, data) -> {
            try {
                return objectMapper.writeValueAsBytes(data);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(String.format("Failed to serialize %s instance", type), e);
            }
        };

        this.deserializer = (topic, data) -> {
            if (data == null) return null;
            try {
                return objectMapper.readValue(data, type);
            } catch (IOException e) {
                throw new IllegalStateException(String.format("Failed to deserialize instance of %s", type), e);
            }
        };
    }

    @Override
    public Serializer<T> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<T> deserializer() {
        return deserializer;
    }
}
