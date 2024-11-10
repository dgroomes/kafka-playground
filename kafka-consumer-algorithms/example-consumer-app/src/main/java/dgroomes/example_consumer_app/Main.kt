package dgroomes.example_consumer_app

import dgroomes.kafka_consumer_concurrent_across_partitions.KafkaConsumerConcurrentAcrossPartitions
import dgroomes.kafka_consumer_concurrent_across_partitions_within_same_poll.KafkaConsumerConcurrentAcrossPartitionsWithinSamePoll
import dgroomes.kafka_consumer_sequential.KafkaConsumerSequential
import dgroomes.kafka_consumer_concurrent_across_keys_with_coroutines.KafkaConsumerConcurrentAcrossKeysWithCoroutines
import dgroomes.kafka_consumer_concurrent_across_keys.KafkaConsumerConcurrentAcrossKeys
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
    val kafkaConsumer = kafkaConsumer()

    // Subscribe and set up a "seek to the end of the topic" operation. Unfortunately this is complicated. It's less
    // complicated if you don't use group management, but I feel like I want to use group management. But maybe I should
    // let that go for the sake of an effective demo.
    val latch = CountDownLatch(1)
    kafkaConsumer.subscribe(listOf(INPUT_TOPIC), object : ConsumerRebalanceListener {
        override fun onPartitionsRevoked(partitions: Collection<TopicPartition>) {}
        override fun onPartitionsAssigned(partitions: Collection<TopicPartition>) {
            val assignment = kafkaConsumer.assignment()
            kafkaConsumer.seekToEnd(assignment) // Warning: this is lazy
            assignment.forEach { partition ->
                // Calling 'position' will force the consumer to actually do the "seek to end" operation.
                val position = kafkaConsumer.position(partition)
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
        "in-process-compute:sequential" -> {
            val processor = PrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer = KafkaConsumerSequential(
                POLL_DELAY,
                kafkaConsumer,
                processor::process
            )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        "in-process-compute:concurrent-across-partitions-within-same-poll" -> {
            val processor = PrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer =
                KafkaConsumerConcurrentAcrossPartitionsWithinSamePoll(
                    POLL_DELAY,
                    kafkaConsumer,
                    processor::process
                )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        "in-process-compute:concurrent-across-partitions" -> {
            val processor = PrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer =
                KafkaConsumerConcurrentAcrossPartitions(
                    POLL_DELAY,
                    COMMIT_DELAY,
                    kafkaConsumer,
                    processor::process
                )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        "in-process-compute:concurrent-across-keys" -> {
            val processor = PrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer =
                KafkaConsumerConcurrentAcrossKeys(
                    INPUT_TOPIC,
                    POLL_DELAY,
                    COMMIT_DELAY,
                    kafkaConsumer,
                    processor::process
                )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        "in-process-compute:concurrent-across-keys-with-coroutines" -> {
            val processor = SuspendingPrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer = KafkaConsumerConcurrentAcrossKeysWithCoroutines(
                INPUT_TOPIC,
                POLL_DELAY,
                kafkaConsumer,
                processor::process,
                COMMIT_DELAY
            )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        "remote-compute:sequential" -> {
            val processor = RemotePrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer = KafkaConsumerSequential(
                POLL_DELAY,
                kafkaConsumer,
                processor::process
            )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        "remote-compute:concurrent-across-partitions-within-same-poll" -> {
            val processor = RemotePrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer =
                KafkaConsumerConcurrentAcrossPartitionsWithinSamePoll(
                    POLL_DELAY,
                    kafkaConsumer,
                    processor::process
                )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        "remote-compute:concurrent-across-partitions" -> {
            val processor = RemotePrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer =
                KafkaConsumerConcurrentAcrossPartitions(
                    POLL_DELAY,
                    COMMIT_DELAY,
                    kafkaConsumer,
                    processor::process
                )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        "remote-compute:concurrent-across-keys" -> {
            val processor = RemotePrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer =
                KafkaConsumerConcurrentAcrossKeys(
                    INPUT_TOPIC,
                    POLL_DELAY,
                    COMMIT_DELAY,
                    kafkaConsumer,
                    processor::process
                )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        "remote-compute:concurrent-across-keys-with-coroutines" -> {
            val processor = SuspendingRemotePrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer = KafkaConsumerConcurrentAcrossKeysWithCoroutines(
                INPUT_TOPIC,
                POLL_DELAY,
                kafkaConsumer,
                processor::process,
                COMMIT_DELAY
            )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        else -> {
            System.out.printf(
                """
                Expected one of:
                    "in-process-compute:sequential"
                    "in-process-compute:concurrent-across-partitions-within-same-poll"
                    "in-process-compute:concurrent-across-partitions"
                    "in-process-compute:concurrent-across-keys"
                    "in-process-compute:concurrent-across-keys-with-coroutines"
                    "remote-compute:sequential"
                    "remote-compute:concurrent-across-partitions-within-same-poll"
                    "remote-compute:concurrent-across-partitions"
                    "remote-compute:concurrent-across-keys"
                    "remote-compute:concurrent-across-keys-with-coroutines"
                    
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
