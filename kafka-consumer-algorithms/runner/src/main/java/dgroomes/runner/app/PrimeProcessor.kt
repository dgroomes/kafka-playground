package dgroomes.runner.app

import dgroomes.runner.primes.PrimeFinder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

/**
 * This encapsulates the wiring between our domain logic ("finding the lowest word" in a message) and handles the
 * mechanical work of publishing the result to an output Kafka topic.
 */
class PrimeProcessor(private val producer: KafkaProducer<String, String>, private val outputTopic: String) {

    private val log = LoggerFactory.getLogger("app")

    fun process(record: ConsumerRecord<String, String>) {
        log.debug("Processing record (topic:partition:offset): {}:{}:{}", record.topic(), record.partition(), record.offset())
        val nth = Integer.valueOf(record.value())
        val nthPrime = PrimeFinder.findNthPrime(nth)
        val outRecord = ProducerRecord(outputTopic, record.key(), "The %,d prime number is %,d".format(nth, nthPrime))
        val sent = producer.send(outRecord)
        sent.get()
    }
}
