package dgroomes.kafka_in_kafka_out.app

import dgroomes.kafka_in_kafka_out.kafka_utils.RecordProcessor
import dgroomes.kafka_in_kafka_out.kafka_utils.SuspendingRecordProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This encapsulates the wiring between our domain logic ("finding the lowest word" in a message) and handles the
 * mechanical work of publishing the result to an output Kafka topic.
 */
class AppRecordProcessor(private val producer: KafkaProducer<String, String>, private val outputTopic: String) : RecordProcessor<String, String>,  SuspendingRecordProcessor<String, String>, Closeable {

    private val log = LoggerFactory.getLogger("app")

    override fun close() {
        producer.close()
    }

    override fun process(record: ConsumerRecord<String, String>) {
        val outRecord = doProcess(record)
        val sent = producer.send(outRecord)
        sent.get()
    }

    override suspend fun suspendingProcess(record: ConsumerRecord<String, String>) {
        val outRecord = doProcess(record)
        suspendCoroutine<Unit?> {
            producer.send(outRecord) { _, _ ->
                it.resume(null)
            }
        }
    }

    private fun doProcess(record: ConsumerRecord<String, String>): ProducerRecord<String, String> {
        log.debug("Processing record (partition/offset/key) {}:{}:{}", record.partition(), record.offset(), record.key())
        val nth = Integer.valueOf(record.value())
        val nthPrime = PrimeFinder.findNthPrime(nth)
        val outRecord = ProducerRecord(outputTopic, record.key(), "The %,d prime number is %,d".format(nth, nthPrime))
        return outRecord
    }
}
