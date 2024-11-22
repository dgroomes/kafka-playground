package dgroomes.runner

import dgroomes.kafka_consumer_concurrent_across_keys.KafkaConsumerConcurrentAcrossKeys
import dgroomes.kafka_consumer_concurrent_across_keys_with_coroutines.KafkaConsumerConcurrentAcrossKeysWithCoroutines
import dgroomes.kafka_consumer_concurrent_across_partitions.KafkaConsumerConcurrentAcrossPartitions
import dgroomes.kafka_consumer_concurrent_across_partitions_within_same_poll.KafkaConsumerConcurrentAcrossPartitionsWithinSamePoll
import dgroomes.kafka_consumer_sequential.KafkaConsumerSequential
import dgroomes.runner.Mode.*
import dgroomes.runner.TestHarness.Scenario
import dgroomes.runner.TestHarness.Scenario.*
import dgroomes.runner.app.PrimeProcessor
import dgroomes.runner.app.RemotePrimeProcessor
import dgroomes.runner.app.SuspendingPrimeProcessor
import dgroomes.runner.app.SuspendingRemotePrimeProcessor
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
import kotlin.system.exitProcess

const val KAFKA_BROKER_HOST: String = "localhost:9092"
const val INPUT_TOPIC: String = "input"
const val OUTPUT_TOPIC: String = "output"
val POLL_DELAY: Duration = Duration.ofMillis(500)
val COMMIT_DELAY: Duration = Duration.ofSeconds(1)
val STARTUP_TIMEOUT: Duration = Duration.ofSeconds(15)

val log: Logger = LoggerFactory.getLogger("app")

/**
 * See the README for more information.
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        log.error("Expected at least one argument (read the code)")
        exitProcess(1)
    }

    when (args[0]) {
        "test-one-message" -> {
            TestHarness().use {
                it.setup()
                it.oneMessage()
            }
            return
        }
        "test-multi-message" -> {
            TestHarness().use {
                it.setup()
                it.multiMessage()
            }
            return
        }
        "load-batch" -> {
            TestHarness().use {
                it.setup()
                it.loadBatch()
            }
            return
        }
        "load-batch-uneven" -> {
            TestHarness().use {
                it.setup()
                it.loadBatchUneven()
            }
            return
        }
        "load-steady" -> {
            TestHarness().use {
                it.setup()
                it.loadSteady()
            }
            return
        }
        "test-all-in-one" -> {
            // We can only test the in-process-compute modes because the remote-compute modes fake out the prime number
            // computation so the assertion would fail.
            val inProcessModes = Mode.entries.filter { it.id.contains("in-process-compute") }
            allInOne(inProcessModes, listOf(ONE_MESSAGE, MULTI_MESSAGE))
            return
        }
        "load-all-in-one" -> {
            allInOne(Mode.entries, listOf(LOAD_BATCH, LOAD_BATCH_UNEVEN, LOAD_STEADY))
            return
        }
        "load-scratch" -> {
            allInOne(listOf(REMOTE_SEQUENTIAL, REMOTE_CONCURRENT_PARTITIONS_SAME_POLL, REMOTE_CONCURRENT_PARTITIONS), listOf(LOAD_BATCH, LOAD_STEADY))
            return
        }
    }

    if (args[0] != "standalone") {
        log.error("Unexpected argument (read the code)")
        return
    }

    if (args.size != 2) {
        log.error("The 'standalone' argument requires a 'mode' argument (read the code).")
        exitProcess(1)
    }

    val mode = parseMode(args[1])
    val consumerCloseable = appConsumer(mode)

    Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("shutdown-hook").unstarted {
        // Note: we don't use the logging framework here because it may have been shutdown already. We have to use
        // print statements.
        println("Shutdown hook triggered. Shutting down the program components.")
        try {
            consumerCloseable.close()
        } catch (e: Exception) {
            println("Failed to close the high level consumer")
            e.printStackTrace()
        }
    })
}

fun parseMode(mode: String): Mode {
    when (val modeEnum = Mode.entries.find { it.id == mode }) {
        null -> {
            System.out.printf(
                """
                Expected one of:
                %s
                    
                but found '%s'.%n
                """.trimIndent().format(Mode.entries.joinToString("\n") { it.id }.prependIndent(), mode))
            exitProcess(1)
        }
        else -> return modeEnum
    }
}

fun allInOne(modes: List<Mode>, scenarios: List<Scenario>) {
    for (mode in modes) {
        appConsumer(mode).use {
            TestHarness().use { harness ->
                harness.setup()
                for (scenario in scenarios) {
                    harness.run(scenario)
                }
            }
        }
    }
}

fun appConsumer(mode: Mode) : Closeable {
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
        IN_PROCESS_SEQUENTIAL -> {
            val processor = PrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer = KafkaConsumerSequential(
                POLL_DELAY,
                kafkaConsumer,
                processor::process
            )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        IN_PROCESS_CONCURRENT_PARTITIONS_SAME_POLL -> {
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

        IN_PROCESS_CONCURRENT_PARTITIONS -> {
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

        IN_PROCESS_CONCURRENT_KEYS -> {
            val processor = PrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer =
                KafkaConsumerConcurrentAcrossKeys(
                    POLL_DELAY,
                    COMMIT_DELAY,
                    kafkaConsumer,
                    processor::process
                )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        IN_PROCESS_CONCURRENT_KEYS_COROUTINES -> {
            val processor = SuspendingPrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer = KafkaConsumerConcurrentAcrossKeysWithCoroutines(
                POLL_DELAY,
                kafkaConsumer,
                processor::process,
                COMMIT_DELAY
            )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        REMOTE_SEQUENTIAL -> {
            val processor = RemotePrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer = KafkaConsumerSequential(
                POLL_DELAY,
                kafkaConsumer,
                processor::process
            )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        REMOTE_CONCURRENT_PARTITIONS_SAME_POLL -> {
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

        REMOTE_CONCURRENT_PARTITIONS -> {
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

        REMOTE_CONCURRENT_KEYS -> {
            val processor = RemotePrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer =
                KafkaConsumerConcurrentAcrossKeys(
                    POLL_DELAY,
                    COMMIT_DELAY,
                    kafkaConsumer,
                    processor::process
                )

            processorCloseable = consumer
            processorStart = consumer::start
        }

        REMOTE_CONCURRENT_KEYS_COROUTINES -> {
            val processor = SuspendingRemotePrimeProcessor(producer, OUTPUT_TOPIC)
            val consumer = KafkaConsumerConcurrentAcrossKeysWithCoroutines(
                POLL_DELAY,
                kafkaConsumer,
                processor::process,
                COMMIT_DELAY
            )

            processorCloseable = consumer
            processorStart = consumer::start
        }
    }

    processorStart()
    log.info("App is configured to run in mode: '{}'. Waiting for the consumer to be ready...", mode.id)
    if (latch.await(STARTUP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
        log.info("The consumer is ready!")
    } else {
        throw IllegalStateException("Timed out waiting for the consumer to seek to the end of the topic")
    }

    return Closeable {
        producer.close()
        processorCloseable.close()
    }
}

enum class Mode(val id: String) {
    IN_PROCESS_SEQUENTIAL("in-process-compute:sequential"),
    IN_PROCESS_CONCURRENT_PARTITIONS_SAME_POLL("in-process-compute:concurrent-across-partitions-within-same-poll"),
    IN_PROCESS_CONCURRENT_PARTITIONS("in-process-compute:concurrent-across-partitions"),
    IN_PROCESS_CONCURRENT_KEYS("in-process-compute:concurrent-across-keys"),
    IN_PROCESS_CONCURRENT_KEYS_COROUTINES("in-process-compute:concurrent-across-keys-with-coroutines"),
    REMOTE_SEQUENTIAL("remote-compute:sequential"),
    REMOTE_CONCURRENT_PARTITIONS_SAME_POLL("remote-compute:concurrent-across-partitions-within-same-poll"),
    REMOTE_CONCURRENT_PARTITIONS("remote-compute:concurrent-across-partitions"),
    REMOTE_CONCURRENT_KEYS("remote-compute:concurrent-across-keys"),
    REMOTE_CONCURRENT_KEYS_COROUTINES("remote-compute:concurrent-across-keys-with-coroutines");
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
