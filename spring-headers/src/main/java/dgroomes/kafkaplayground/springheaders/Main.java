package dgroomes.kafkaplayground.springheaders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class);
    }

    /**
     * Supply a JsonMessageConverter to the application context to engage the Spring Boot + Spring Kafka
     * (de)serialization machinery to deserialize messages in a way that assumes they are JSON. See the closely related
     * documentation section https://docs.spring.io/spring-kafka/docs/2.5.1.RELEASE/reference/html/#messaging-message-conversion
     * <p>
     * Also, set the underlying "type precendence" configuration to "TYPE_ID" so that the (de)serialization uses the
     * "__TypeId__" header in the Kafka messages to actually determine the target Java type to deserialize to. This is a
     * feature we are opting in to only because we know our input Kafka topic has messages with this header set.
     */
    @Bean
    public JsonMessageConverter converter() {
        var converter = new JsonMessageConverter();
        var typeMapper = converter.getTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);
        typeMapper.addTrustedPackages("dgroomes.kafkaplayground.springheaders.model");
        return converter;
    }
}
