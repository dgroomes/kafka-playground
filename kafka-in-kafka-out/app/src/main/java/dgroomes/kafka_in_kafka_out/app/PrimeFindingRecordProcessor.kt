package dgroomes.kafka_in_kafka_out.app

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

/**
 * This encapsulates the wiring between our domain logic ("finding the lowest word" in a message) and handles the
 * mechanical work of publishing the result to an output Kafka topic.
 */
class PrimeFindingRecordProcessor(private val producer: KafkaProducer<String, String>, private val outputTopic: String) {

    fun process(record: ConsumerRecord<String, String>) {
        val nth = Integer.valueOf(record.value())
        val nthPrime = PrimeFinder.findNthPrime(nth)
        val outRecord = ProducerRecord(outputTopic, record.key(), "The %,d prime number is %,d".format(nth, nthPrime))
        val sent = producer.send(outRecord)
        sent.get()
    }
}
