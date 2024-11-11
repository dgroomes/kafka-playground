package dgroomes.runner.app

import dgroomes.runner.primes.PrimeFinder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This is a coroutines variation of [PrimeProcessor].
 */
class SuspendingPrimeProcessor(private val producer: KafkaProducer<String, String>, private val outputTopic: String) {

    suspend fun process(record: ConsumerRecord<String, String>) {
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
