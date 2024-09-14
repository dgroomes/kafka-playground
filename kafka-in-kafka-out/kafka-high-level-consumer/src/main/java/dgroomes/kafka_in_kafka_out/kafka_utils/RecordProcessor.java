package dgroomes.kafka_in_kafka_out.kafka_utils;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface RecordProcessor<KEY, PAYLOAD> {
    void process(ConsumerRecord<KEY, PAYLOAD> record) throws Exception;
}
