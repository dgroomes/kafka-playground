package dgroomes.kafka_playground.streams_zip_codes;

import dgroomes.streams_zip_codes.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SuppressWarnings("resource")
class TopologyTest {

    TopologyTestDriver driver;
    TestInputTopic<String, ZipArea> input;
    TestOutputTopic<City, CityStats> cityOutput;
    TestOutputTopic<String, StateStats> stateOutput;
    TestOutputTopic<String, OverallStats> overallOutput;

    @BeforeEach
    void before() {
        driver = new TopologyTestDriver(new TopologyBuilder().build(), App.config());

        input = driver.createInputTopic("streams-zip-codes-zip-areas", Serdes.String().serializer(), TopologyBuilder.zipAreaSerde.serializer());
        cityOutput = driver.createOutputTopic("streams-zip-codes-city-stats-changelog", TopologyBuilder.citySerde.deserializer(), TopologyBuilder.cityStatsSerde.deserializer());
        stateOutput = driver.createOutputTopic("streams-zip-codes-state-stats-changelog", Serdes.String().deserializer(), TopologyBuilder.stateStatsSerde.deserializer());
        overallOutput = driver.createOutputTopic("streams-zip-codes-overall-stats-changelog", Serdes.String().deserializer(), TopologyBuilder.overallStatsSerde.deserializer());
    }

    @AfterEach
    void close() {
        driver.close();
    }

    /**
     * The topology should compute an average of ZIP area populations for each city
     */
    @Test
    void averageByCity() {
        input.pipeInput(new ZipArea("1", "SPRINGFIELD", "MA", 1));
        input.pipeInput(new ZipArea("2", "SPRINGFIELD", "MA", 3));

        var outputRecords = cityOutput.readRecordsToList();

        assertThat(outputRecords)
                .extracting("key", "value")
                .containsExactly(
                        tuple(new City("SPRINGFIELD", "MA"), new CityStats(1, 1, 1)),
                        tuple(new City("SPRINGFIELD", "MA"), new CityStats(2, 4, 2)));
    }

    /**
     * The topology should compute an average of ZIP area populations for each state
     */
    @Test
    void averageByState() {
        input.pipeInput(new ZipArea("1", "SPRINGFIELD", "MA", 1));
        input.pipeInput(new ZipArea("2", "BOSTON", "MA", 3));

        var outputRecords = stateOutput.readRecordsToList();

        assertThat(outputRecords)
                .extracting("key", "value")
                .containsExactly(
                        tuple("MA", new StateStats(1, 1, 1)),
                        tuple("MA", new StateStats(2, 4, 2)));
    }

    @Test
    void averageOverall() {
        input.pipeInput(new ZipArea("1", "SPRINGFIELD", "MA", 1));
        input.pipeInput(new ZipArea("2", "PROVIDENCE", "RI", 3));

        var outputRecords = overallOutput.readRecordsToList();

        assertThat(outputRecords)
                .extracting("key", "value")
                .containsExactly(
                        tuple("USA", new OverallStats(1, 1, 1)),
                        tuple("USA", new OverallStats(2, 4, 2)));
    }

    /**
     * Records for already-seen keys should replace the original record.
     */
    @Test
    void sameKeyUpdates() {
        input.pipeInput(new ZipArea("1", "SPRINGFIELD", "MA", 1));
        input.pipeInput(new ZipArea("1", "SPRINGFIELD", "MA", 2));

        var outputRecords = cityOutput.readRecordsToList();

        assertThat(outputRecords)
                .map(record -> tuple(record.key(), record.value()))
                .containsExactly(
                        tuple(new City("SPRINGFIELD", "MA"), new CityStats(1, 1, 1)),
                        tuple(new City("SPRINGFIELD", "MA"), new CityStats(1, 2, 2)));
    }
}
