package dgroomes.kafka_in_kafka_out.app

import kafka_in_kafka_out.kafka_utils.HighLevelConsumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

/**
 * See the README for more information.
 */
object Main {
    val log: Logger = LoggerFactory.getLogger(Main::class.java)
    const val KAFKA_BROKER_HOST: String = "localhost:9092"
    const val INPUT_TOPIC: String = "input-text"
    val pollDuration: Duration = Duration.ofMillis(200)
    const val OUTPUT_TOPIC: String = "lowest-word"
    val reportingDelay: Duration = Duration.ofSeconds(2)

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1) { "Expected exactly one argument: 'sync', 'async', or 'async-coroutines'" }
        val mode = args[0]

        val consumer = kafkaConsumer()
        val producer = kafkaProducer()

        val highLevelConsumer: HighLevelConsumer
        val appProcessor = AppRecordProcessor(producer)
        val suspendingAppRecordProcessor = SuspendingAppRecordProcessor(producer)

        highLevelConsumer = when (mode) {
            "sync" -> HighLevelConsumer.syncConsumer(
                INPUT_TOPIC,
                pollDuration,
                consumer,
                appProcessor
            )

            "async" -> HighLevelConsumer.asyncConsumerVirtualThreads(
                INPUT_TOPIC,
                pollDuration,
                consumer,
                appProcessor,
                reportingDelay
            )

            "async-coroutines" -> HighLevelConsumer.asyncConsumerCoroutines(
                INPUT_TOPIC,
                pollDuration,
                consumer,
                appProcessor,
                reportingDelay
            )

            "async-coroutines-flow" -> HighLevelConsumer.asyncConsumerCoroutinesFlow(
                INPUT_TOPIC,
                pollDuration,
                consumer,
                suspendingAppRecordProcessor,
                reportingDelay
            )

            else -> {
                System.out.printf("Expected 'sync', 'async' or 'async-coroutines' but found '%s'.%n", mode)
                return
            }
        }

        log.info(
            "Starting the high-level '{}' processor. This is a simple stateless transformation program: Kafka in, Kafka out.",
            mode
        )
        highLevelConsumer.start()

        Runtime.getRuntime().addShutdownHook(Thread {
            // Note: we don't use the logging framework here because it may have been shutdown already. We have to use
            // print statements.
            println("Shutdown hook triggered. Shutting down the program components.")
            try {
                highLevelConsumer.close()
            } catch (e: Exception) {
                println("Failed to close the high level consumer")
                e.printStackTrace()
            }

            try {
                consumer.close()
            } catch (e: Exception) {
                println("Failed to close the Kafka consumer")
                e.printStackTrace()
            }
            try {
                producer.close()
            } catch (e: Exception) {
                println("Failed to close the Kafka producer")
                e.printStackTrace()
            }
        })
    }

    /**
     * Construct a KafkaConsumer
     */
    fun kafkaConsumer(): KafkaConsumer<Int, String> {
        val config = Properties()
        config["group.id"] = "my-group"
        config["bootstrap.servers"] = KAFKA_BROKER_HOST
        config["key.deserializer"] = "org.apache.kafka.common.serialization.IntegerDeserializer"
        config["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        config["max.poll.records"] = 100
        return KafkaConsumer(config)
    }

    /**
     * Construct a KafkaProducer
     */
    fun kafkaProducer(): KafkaProducer<Int, String> {
        val props = Properties()
        props["bootstrap.servers"] = KAFKA_BROKER_HOST
        props["acks"] = "all"
        props["key.serializer"] = "org.apache.kafka.common.serialization.IntegerSerializer"
        props["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        return KafkaProducer(props)
    }
}