package dgroomes.kafka_in_kafka_out.app;

import dgroomes.kafka_in_kafka_out.kafka_utils.RecordProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.Closeable;

/**
 * This encapsulates the wiring between our domain logic ("finding the lowest word" in a message) and handles the
 * mechanical work of publishing the result to an output Kafka topic.
 */
public class AppRecordProcessor implements RecordProcessor<Integer, String>, Closeable {

    private final KafkaProducer<Integer, String> producer;

    public AppRecordProcessor(KafkaProducer<Integer, String> producer) {
        this.producer = producer;
    }

    @Override
    public void close() {
        if (producer != null) producer.close();
    }

    @Override
    public void process(ConsumerRecord<Integer, String> record) throws Exception {
        int sortFactor;
        var header = record.headers().lastHeader("sort_factor");
        if (header == null) {
            sortFactor = 1;
        } else {
            sortFactor = Integer.parseInt(new String(header.value()));
        }
        var lowest = LowestWord.lowest(record.value(), sortFactor);
        var outRecord = new ProducerRecord<>(Main.OUTPUT_TOPIC, record.key(), lowest);
        var sent = producer.send(outRecord);
        sent.get();
    }
}
