package dgroomes.kafka_playground.streams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class TopologyTest {

    TopologyTestDriver driver;

    @BeforeEach
    void before() {
        driver = new TopologyTestDriver(App.topology(), App.config());
    }

    @AfterEach
    void close() {
        driver.close();
    }

    @Test
    void test() {
        var stringSerde = Serdes.String();

        var input = driver.createInputTopic("plaintext-input", stringSerde.serializer(), stringSerde.serializer());
        var output = driver.createOutputTopic("streams-wordcount-output", stringSerde.deserializer(), Serdes.Long().deserializer());

        input.pipeInput("hello world!");
        input.pipeInput("hello good world!");
        input.pipeInput("hello great world!");

        var outputRecords = output.readRecordsToList();

        assertThat(outputRecords)
                .extracting("key", "value")
                .containsExactly(
                        tuple("hello", 1L),
                        tuple("world!", 1L),

                        tuple("hello", 2L),
                        tuple("good", 1L),
                        tuple("world!", 2L),

                        tuple("hello", 3L),
                        tuple("great", 1L),
                        tuple("world!", 3L));
    }
}
