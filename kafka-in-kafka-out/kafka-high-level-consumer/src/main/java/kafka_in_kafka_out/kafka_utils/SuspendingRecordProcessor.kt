package kafka_in_kafka_out.kafka_utils

import org.apache.kafka.clients.consumer.ConsumerRecord

interface SuspendingRecordProcessor<KEY, PAYLOAD> {

    @Throws(Exception::class)
    suspend fun process(record: ConsumerRecord<KEY, PAYLOAD>)
}
