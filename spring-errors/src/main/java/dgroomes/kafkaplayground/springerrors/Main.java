package dgroomes.kafkaplayground.springerrors;

import dgroomes.kafkaplayground.springerrors.model.Message;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.FailedDeserializationInfo;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@SpringBootApplication
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        SpringApplication.run(Main.class);
    }

    /**
     * Catch deserialization errors. See https://docs.spring.io/spring-kafka/reference/html/#error-handling-deserializer
     * <p>
     * Also, configure a JSON serializer (Spring Boot + Spring Kafka does not assume that the Kafka messages are JSON,
     * so we must configure it).
     */
    @Bean
    public DefaultKafkaConsumerFactoryCustomizer consumerCustomizerForDeserialization() {
        return consumerFactory -> {
            var jsonDeserializer = new JsonDeserializer<>(Message.class);
            var errorHandlingDeserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);
            errorHandlingDeserializer.setFailedDeserializationFunction(new Function<FailedDeserializationInfo, Message>() {
                @Override
                public Message apply(FailedDeserializationInfo failedDeserializationInfo) {
                    String valueString;
                    var data = failedDeserializationInfo.getData();
                    valueString = new String(data);
                    log.error("Error deserializing record for value {}", valueString, failedDeserializationInfo.getException());
                    return null;
                }
            });
            consumerFactory.setValueDeserializer((Deserializer) errorHandlingDeserializer);
        };
    }

    @Bean
    public KafkaTemplate<?, ?> stringTemplate(
            KafkaProperties properties) {
        DefaultKafkaProducerFactory<?, ?> factory = new DefaultKafkaProducerFactory<>(
                properties.buildProducerProperties());
        factory.setValueSerializer((Serializer) new StringSerializer());
        KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate(factory);
        return kafkaTemplate;
    }

    @Bean
    public KafkaTemplate<?, ?> bytesTemplate(KafkaProperties properties) {
        DefaultKafkaProducerFactory<?, ?> factory = new DefaultKafkaProducerFactory<>(
                properties.buildProducerProperties());
        factory.setValueSerializer((Serializer) new ByteArraySerializer());
        KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate(factory);
        return kafkaTemplate;
    }

    /**
     * Supply an error handler. It should get picked up by the Spring Boot + Spring Kafka machinery and applied into
     * our Kafka listener. See https://github.com/spring-projects/spring-boot/blob/837947ca090cc18934379a0142c8ba0fa9b3f0f5/spring-boot-project/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/kafka/KafkaAnnotationDrivenConfiguration.java#L90
     * <p>
     * I don't really understand why I need a KafkaTemplate that is backed by a ByteArraySerializer. Instead, can I use
     * a KafkaTemplate that's backed just by a simple StringSerializer? Do I have to work with bytes? But it's what
     * worked. See the "DeadLetterPublishingRecoverer" example at https://docs.spring.io/spring-kafka/docs/3.0.1/reference/html/#dead-letters
     */
    @Bean
    public CommonErrorHandler deadLetterErrorHandler(
            KafkaTemplate<?, ?> stringTemplate,
            KafkaTemplate<?, ?> bytesTemplate) {
        Map<Class<?>, KafkaTemplate<?, ?>> templates = new LinkedHashMap<>();
        templates.put(String.class, stringTemplate);
        templates.put(byte[].class, bytesTemplate);
        var recoverer = new DeadLetterPublishingRecoverer((Map) templates);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 1L));
    }
}
