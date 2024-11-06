package dgroomes.kafka_in_kafka_out.app

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This is the coroutines version of [PrimeFindingRecordProcessor].
 */
class SuspendingPrimeFindingRecordProcessor(private val producer: KafkaProducer<String, String>, private val outputTopic: String) {

    suspend fun suspendingProcess(record: ConsumerRecord<String, String>) {
        val nth = Integer.valueOf(record.value())
        val nthPrime = PrimeFinder.findNthPrime(nth)
        val outRecord = ProducerRecord(outputTopic, record.key(), "The %,d prime number is %,d".format(nth, nthPrime))
        suspendCoroutine<Unit?> {
            producer.send(outRecord) { _, _ ->
                it.resume(null)
            }
        }
    }
}
