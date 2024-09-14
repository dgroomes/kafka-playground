package dgroomes.kafka_in_kafka_out.app

import dgroomes.kafka_in_kafka_out.kafka_utils.SuspendingRecordProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This encapsulates the domain logic of the compute-intensive operation of "finding the lowest word" in a message. It
 * also handles the mechanical work of publishing the result to an output Kafka topic.
 */
class SuspendingAppRecordProcessor(private val producer: KafkaProducer<Int, String>) :
    SuspendingRecordProcessor<Int, String>, Closeable {

    private val log = LoggerFactory.getLogger(SuspendingAppRecordProcessor::class.java)

    @Throws(Exception::class)
    override suspend fun process(record: ConsumerRecord<Int, String>) {
        val header = record.headers().lastHeader("sort_factor")
        val sortFactor = if (header == null) {
            1
        } else {
            String(header.value()).toInt()
        }
        val lowest = LowestWord.lowest(record.value(), sortFactor)
        val outRecord = ProducerRecord(Main.OUTPUT_TOPIC, record.key(), lowest)

        suspendCoroutine<Unit?> {
            producer.send(outRecord) { _, exception ->
                if (exception != null) {
                    log.error("Failed to send record to Kafka", exception)
                }
                it.resume(null)
            }
        }
    }

    override fun close() {
        producer.close()
    }
}
