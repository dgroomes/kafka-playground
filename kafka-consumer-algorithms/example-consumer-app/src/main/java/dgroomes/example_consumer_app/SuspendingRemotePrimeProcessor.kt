package dgroomes.example_consumer_app

import kotlinx.coroutines.delay
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Duration
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.toKotlinDuration

/**
 * This is a coroutines variation of [RemotePrimeProcessor].
 */
class SuspendingRemotePrimeProcessor(
    private val producer: KafkaProducer<String, String>,
    private val outputTopic: String
) {
    suspend fun process(record: ConsumerRecord<String, String>) {
        val nth = record.value().toInt()

        val sleep: Duration
        val prime: Int
        if (nth <= 10000) {
            sleep = Duration.ofMillis(2)
            prime = 104729
        } else if (nth <= 100000) {
            sleep = Duration.ofMillis(50)
            prime = 1299709
        } else {
            sleep = Duration.ofSeconds(1)
            prime = 15485863
        }

        delay(sleep.toKotlinDuration())

        val msg = String.format("The %,d prime number is %,d (faked value)", nth, prime)
        val outRecord = ProducerRecord(outputTopic, record.key(), msg)
        suspendCoroutine<Unit?> {
            producer.send(outRecord) { _, _ ->
                it.resume(null)
            }
        }
    }
}
