package dgroomes.kafka_in_kafka_out.app

import dgroomes.kafka_in_kafka_out.kafka_utils.RecordProcessor
import dgroomes.kafka_in_kafka_out.kafka_utils.SuspendingRecordProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This encapsulates the wiring between our domain logic ("finding the lowest word" in a message) and handles the
 * mechanical work of publishing the result to an output Kafka topic.
 */
class AppRecordProcessor(private val producer: KafkaProducer<Int, String>, private val outputTopic: String) : RecordProcessor<Int, String>,  SuspendingRecordProcessor<Int, String>, Closeable {
    override fun close() {
        producer.close()
    }

    override fun process(record: ConsumerRecord<Int, String>) {
        val outRecord = doProcess(record)
        val sent = producer.send(outRecord)
        sent.get()
    }

    override suspend fun suspendingProcess(record: ConsumerRecord<Int, String>) {
        val outRecord = doProcess(record)
        suspendCoroutine<Unit?> {
            producer.send(outRecord) { _, _ ->
                it.resume(null)
            }
        }
    }

    private fun doProcess(record: ConsumerRecord<Int, String>): ProducerRecord<Int, String> {
        val sortFactor: Int
        val header = record.headers().lastHeader("sort_factor")
        sortFactor = if (header == null) {
            1
        } else {
            String(header.value()).toInt()
        }
        val lowest = LowestWord.lowest(record.value(), sortFactor)
        val outRecord = ProducerRecord(outputTopic, record.key(), lowest)
        return outRecord
    }
}
