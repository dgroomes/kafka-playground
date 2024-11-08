package dgroomes.example_consumer_app

import dgroomes.kafka_consumer_batch.KeyBasedBatchConsumer
import dgroomes.kafka_consumer_with_coroutines.KeyBasedAsyncConsumerWithCoroutines
import dgroomes.virtual_thread_kafka_consumer.KeyBasedAsyncConsumerWithVirtualThreads
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.TopicPartition
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Duration
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.collections.set

const val KAFKA_BROKER_HOST: String = "localhost:9092"
const val INPUT_TOPIC: String = "input"
const val OUTPUT_TOPIC: String = "output"
val POLL_DELAY: Duration = Duration.ofMillis(500)
val COMMIT_DELAY: Duration = Duration.ofSeconds(1)
val STARTUP_TIMEOUT: Duration = Duration.ofSeconds(10)

val log: Logger = LoggerFactory.getLogger("app")

/**
 * See the README for more information.
 */
fun main(args: Array<String>) {
    require(args.size == 1) { "Expected exactly one argument: <compute:consumer>. Found '${args.joinToString()}'" }
    val mode = args[0]
    val consumer = kafkaConsumer()

    // Subscribe and set up a "seek to the end of the topic" operation. Unfortunately this is complicated. It's less
    // complicated if you don't use group management, but I feel like I want to use group management. But maybe I should
    // let that go for the sake of an effective demo.
    val latch = CountDownLatch(1)
    consumer.subscribe(listOf(INPUT_TOPIC), object : ConsumerRebalanceListener {
        override fun onPartitionsRevoked(partitions: Collection<TopicPartition>) {}
        override fun onPartitionsAssigned(partitions: Collection<TopicPartition>) {
            val assignment = consumer.assignment()
            consumer.seekToEnd(assignment) // Warning: this is lazy
            assignment.forEach { partition ->
                // Calling 'position' will force the consumer to actually do the "seek to end" operation.
                val position = consumer.position(partition)
                log.debug("Seeked to the end of partition: {} (position: {})", partition, position)
            }
            latch.countDown()
        }
    })
    val producer = kafkaProducer()

    val processorCloseable : Closeable
    val processorStart : () -> Unit

    // Somewhat absurdly verbose code to parse the command line arguments and construct the high-level consumer, but it
    // is easily interpreted.
    when (mode) {
        "in-process-compute:batch-consumer" -> {
            val processor = PrimeProcessor(producer, OUTPUT_TOPIC)
            val batchConsumer = KeyBasedBatchConsumer(
                POLL_DELAY,
                consumer,
                processor::process
            )

            processorCloseable = batchConsumer
            processorStart = batchConsumer::start
        }

        "in-process-compute:virtual-threads-consumer" -> {
            val processor = PrimeProcessor(producer, OUTPUT_TOPIC)
            val virtualThreadsConsumer = KeyBasedAsyncConsumerWithVirtualThreads(
                INPUT_TOPIC,
                POLL_DELAY,
                COMMIT_DELAY,
                consumer,
                processor::process
            )

            processorCloseable = virtualThreadsConsumer
            processorStart = virtualThreadsConsumer::start
        }

        "in-process-compute:coroutines-consumer" -> {
            val processor = SuspendingPrimeProcessor(producer, OUTPUT_TOPIC)
            val coroutinesConsumer = KeyBasedAsyncConsumerWithCoroutines(
                INPUT_TOPIC,
                POLL_DELAY,
                consumer,
                processor::process,
                COMMIT_DELAY
            )

            processorCloseable = coroutinesConsumer
            processorStart = coroutinesConsumer::start
        }

        "remote-compute:batch-consumer" -> {
            val processor = RemotePrimeProcessor(producer, OUTPUT_TOPIC)
            val syncConsumer = KeyBasedBatchConsumer(
                POLL_DELAY,
                consumer,
                processor::process
            )

            processorCloseable = syncConsumer
            processorStart = syncConsumer::start
        }

        "remote-compute:virtual-threads-consumer" -> {
            val processor = RemotePrimeProcessor(producer, OUTPUT_TOPIC)
            val virtualThreadsConsumer = KeyBasedAsyncConsumerWithVirtualThreads(
                INPUT_TOPIC,
                POLL_DELAY,
                COMMIT_DELAY,
                consumer,
                processor::process
            )

            processorCloseable = virtualThreadsConsumer
            processorStart = virtualThreadsConsumer::start
        }

        "remote-compute:coroutines-consumer" -> {
            val processor = SuspendingRemotePrimeProcessor(producer, OUTPUT_TOPIC)
            val coroutinesConsumer = KeyBasedAsyncConsumerWithCoroutines(
                INPUT_TOPIC,
                POLL_DELAY,
                consumer,
                processor::process,
                COMMIT_DELAY
            )

            processorCloseable = coroutinesConsumer
            processorStart = coroutinesConsumer::start
        }

        else -> {
            System.out.printf(
                """
                Expected one of:
                
                    "in-process-compute:batch-consumer"
                    "in-process-compute:virtual-threads-consumer"
                    "in-process-compute:coroutines-consumer"
                    "remote-compute:batch-consumer"
                    "remote-compute:virtual-threads-consumer"
                    "remote-compute:coroutines-consumer"
                    
                but found '%s'.%n",
                """.trimIndent().format(mode)
            )
            return
        }
    }

    Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("shutdown-hook").unstarted {
        // Note: we don't use the logging framework here because it may have been shutdown already. We have to use
        // print statements.
        println("Shutdown hook triggered. Shutting down the program components.")
        try {
            processorCloseable.close()
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

    processorStart()
    log.info("App is configured to run in mode: '{}'. Waiting for the consumer to be ready...", mode)
    if (latch.await(STARTUP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
        log.info("The consumer is ready!")
    } else {
        throw IllegalStateException("Timed out waiting for the consumer to seek to the end of the topic")
    }
}

/**
 * Construct a KafkaConsumer
 */
fun kafkaConsumer(): KafkaConsumer<String, String> {
    val config = Properties()
    config["bootstrap.servers"] = KAFKA_BROKER_HOST
    config["enable.auto.commit"] = false
    config["group.id"] = "app"
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
