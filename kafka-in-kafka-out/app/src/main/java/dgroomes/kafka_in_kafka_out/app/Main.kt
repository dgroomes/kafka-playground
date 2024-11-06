package dgroomes.kafka_in_kafka_out.app

import dgroomes.kafka_consumer_synchronous.KeyBasedSyncConsumer
import dgroomes.kafka_consumer_with_coroutines.KeyBasedAsyncConsumerWithCoroutines
import dgroomes.virtual_thread_kafka_consumer.KeyBasedAsyncConsumerWithVirtualThreads
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Duration
import java.util.*

const val KAFKA_BROKER_HOST: String = "localhost:9092"
const val INPUT_TOPIC: String = "input"
const val OUTPUT_TOPIC: String = "output"
val POLL_DELAY: Duration = Duration.ofMillis(500)
val COMMIT_DELAY: Duration = Duration.ofSeconds(1)
val REPORTING_DELAY: Duration = Duration.ofSeconds(2)

val log: Logger = LoggerFactory.getLogger("app")

/**
 * See the README for more information.
 */
fun main(args: Array<String>) {
    require(args.size == 1) { "Expected exactly one argument: 'sync', 'async-virtual-threads', or 'async-coroutines'. Found '${args.joinToString()}'" }
    val mode = args[0]
    val consumer = kafkaConsumer()
    val producer = kafkaProducer()

    val highLevelConsumer: HighLevelConsumer = when (mode) {
        "sync" -> {
            val processor = PrimeFindingRecordProcessor(producer, OUTPUT_TOPIC)
            val syncConsumer = KeyBasedSyncConsumer(
                INPUT_TOPIC,
                POLL_DELAY,
                consumer,
                processor::process,
                REPORTING_DELAY
            )

            object : HighLevelConsumer {
                override fun close() {
                    syncConsumer.close()
                }

                override fun start() {
                    syncConsumer.start()
                }
            }
        }

        "async-virtual-threads" -> {
            val processor = PrimeFindingRecordProcessor(producer, OUTPUT_TOPIC)
            val virtualThreadsConsumer = KeyBasedAsyncConsumerWithVirtualThreads(
                INPUT_TOPIC,
                POLL_DELAY,
                COMMIT_DELAY,
                REPORTING_DELAY,
                consumer,
                processor::process
            )

            object : HighLevelConsumer {
                override fun close() {
                    virtualThreadsConsumer.close()
                }

                override fun start() {
                    virtualThreadsConsumer.start()
                }
            }
        }

        "async-coroutines" -> {
            val processor = SuspendingPrimeFindingRecordProcessor(producer, OUTPUT_TOPIC)
            val coroutinesConsumer = KeyBasedAsyncConsumerWithCoroutines(
                INPUT_TOPIC,
                POLL_DELAY,
                consumer,
                processor::suspendingProcess,
                REPORTING_DELAY,
                COMMIT_DELAY
            )

            object : HighLevelConsumer {
                override fun close() {
                    coroutinesConsumer.close()
                }

                override fun start() {
                    coroutinesConsumer.start()
                }
            }
        }

        else -> {
            System.out.printf(
                "Expected 'sync', 'async-virtual-threads' or 'async-coroutines' but found '%s'.%n",
                mode
            )
            return
        }
    }

    Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("shutdown-hook").unstarted {
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
            producer.close()
        } catch (e: Exception) {
            println("Failed to close the Kafka producer")
            e.printStackTrace()
        }
    })

    log.info(
        "Starting the high-level '{}' processor. This is a simple stateless transformation program: Kafka in, Kafka out.",
        mode
    )
    highLevelConsumer.start()
}

/**
 * Construct a KafkaConsumer
 */
fun kafkaConsumer(): KafkaConsumer<String, String> {
    val config = Properties()
    config["bootstrap.servers"] = KAFKA_BROKER_HOST
    config["enable.auto.commit"] = false
    config["group.id"] = "my-group"
    config["heartbeat.interval.ms"] = 250
    config["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
    config["max.poll.records"] = 100
    config["session.timeout.ms"] = 1000
    config["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
    return KafkaConsumer(config)
}

/**
 * Construct a KafkaProducer
 */
fun kafkaProducer(): KafkaProducer<String, String> {
    val props = Properties()
    props["acks"] = "all"
    props["bootstrap.servers"] = KAFKA_BROKER_HOST
    props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
    props["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
    return KafkaProducer(props)
}


/**
 * A high-level Kafka consumer. It owns the "poll" loop and encapsulates the mechanics of scheduling message handling
 * work, and commiting offsets.
 */
interface HighLevelConsumer : Closeable {

    /**
     * Start the consumer.
     */
    fun start()
}
