package dgroomes.kafka_in_kafka_out.kafka_utils

import kotlinx.coroutines.*
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.toKotlinDuration

/**
 * An asynchronous Kafka consumer and message processor implemented with coroutines.
 *
 * Messages are processed in "key order". This means that a message with a key of "xyz" will always be processed before
 * a later message with a key of "xyz". Messages with the same key are assumed to be on the same partition.
 *
 * Notice: This implementation does not do error handling, it is not careful about shutdown semantics, and it does not
 * handle Kafka consumer group re-balances and other cases. A production implementation must take more care.
 *
 * The logical algorithm is designed for high bandwidth and low latency. Specifically, we've designed for these traits:
 *
 *   - Message processing is concurrent
 *   - Message processing related to one key does not block message processing related to another key
 *   - Message processing does not block polling
 *   - Message processing does not block offset committing
 *   - Message processing is not confined to the records in a given poll batch
 *   - Offset committing for one partition does not block offset committing for another partition
 *
 * In contrast to the logic, the actual implementation is not necessarily optimized for low latency because it allocates
 * lots of objects and I think spends a lot of cycles on context switching. But that sort of optimization was not my
 * goal. My main goal was to explore the expressiveness of the coroutines programming model. The end result I think is
 * pretty expressive.
 */
class KeyBasedAsyncConsumerWithCoroutines<KEY, PAYLOAD>(
    private val topic: String,
    pollDelay: Duration,
    private val consumer: Consumer<KEY, PAYLOAD>,
    private val handlerFn: SuspendingRecordProcessor<KEY, PAYLOAD>,
    reportingInterval: Duration = Duration.ofMinutes(1),
    commitDelay: Duration
) : HighLevelConsumer {

    private val log = LoggerFactory.getLogger("consumer.coroutines")
    private val orchExecutor: ExecutorService
    private val orchDispatcher: CoroutineDispatcher
    private val orchScope: CoroutineScope
    private val pollDelay: kotlin.time.Duration
    private var processed = 0
    private val commitDelay: kotlin.time.Duration
    private val reportingDelay: kotlin.time.Duration
    private val tailHandlerJobByKey = mutableMapOf<KEY, Job>()
    private val tailOffsetJobByPartition = mutableMapOf<Int, Job>()
    private val nextOffsets = mutableMapOf<Int, Long>()
    private var queueSize = 0
    private val queueDesiredMax = 100
    private var processingTime = Duration.ZERO
    private var processingStart: Instant? = null

    init {
        // "orch" is short for "orchestrator"
        val namedFactory = Thread.ofPlatform().name("consumer-orch").factory()
        orchExecutor = Executors.newSingleThreadExecutor(namedFactory)
        orchDispatcher = orchExecutor.asCoroutineDispatcher()
        orchScope = CoroutineScope(orchDispatcher)
        this.pollDelay = pollDelay.toKotlinDuration()
        this.commitDelay = commitDelay.toKotlinDuration()
        this.reportingDelay = reportingInterval.toKotlinDuration()
    }

    override fun start() {
        consumer.subscribe(listOf(topic))
        orchScope.launch { runEvery(::poll, pollDelay) }
        orchScope.launch { runEvery(::commit, commitDelay) }
        orchScope.launch { runEvery(::report, reportingDelay) }
    }

    private suspend fun CoroutineScope.runEvery(fn: () -> Unit, duration: kotlin.time.Duration) {
        while (isActive) {
            fn()
            delay(duration)
        }
    }

    private fun poll() {
        if (queueSize > queueDesiredMax) {
            log.debug("The desired maximum queue is full (%,d). Skipping poll.".format(queueSize))
            return
        }

        while (true) {
            val records = consumer.poll(Duration.ZERO)
            log.debug("Polled {} records", records.count())
            if (records.isEmpty) break
            if (queueSize == 0) processingStart = Instant.now()

            queueSize += records.count()

            for (record in records) {
                // Schedule handler work.
                val key = record.key()
                val tailHandlerJob = tailHandlerJobByKey[key]
                val handlerJob = orchScope.launch(start = CoroutineStart.LAZY) {

                    // Wait for the previous task of the same key to complete. This is our trick for getting in order
                    // process by key. Note: It might be better to put this join inside the same scope that runs the
                    // handler, but we know this doesn't block, so I think it's fine.
                    tailHandlerJob?.join()

                    // "Thread confinement" - move off of the orchestrator thread so that the handler is not
                    // able to monopolize the thread. The handler can block and do as much work as it wants and
                    // the orchestrator will continue to be scheduled thanks to time slicing.
                    withContext(Dispatchers.IO) {
                        handlerFn.suspendingProcess(record)
                    }

                    // Clean up the reference to the tail job, unless another job has taken its place.
                    if (tailHandlerJobByKey[key] == currentCoroutineContext().job) tailHandlerJobByKey.remove(key)

                    processed++
                    queueSize--
                    if (queueSize == 0) processingTime += Duration.between(processingStart, Instant.now())
                }
                tailHandlerJobByKey[key] = handlerJob

                // Schedule offset tracking work.
                val partition = record.partition()
                val tailOffsetJob = tailOffsetJobByPartition[partition]
                val offsetJob = orchScope.launch(start = CoroutineStart.LAZY) {
                    handlerJob.join()
                    tailOffsetJob?.join()
                    nextOffsets[partition] = record.offset() + 1
                    // We don't bother cleaning up the reference to the tail offset job because there is only a small
                    // amount of partitions, whereas we do care about cleaning up the handler job because there could
                    // be a huge number of keys.
                }
                tailOffsetJobByPartition[partition] = offsetJob

                handlerJob.start()
                offsetJob.start()
            }

            if (queueSize > queueDesiredMax) {
                log.debug("Poll filled the queue (%,d) beyond the desired maximum size (%,d).".format(queueSize, queueDesiredMax))
                return
            }
        }
    }

    private fun commit() {
        if (nextOffsets.isEmpty()) return

        val toCommit = nextOffsets.map { (partition, offset) ->
            TopicPartition(topic, partition) to OffsetAndMetadata(offset)
        }.toMap()

        nextOffsets.clear()

        consumer.commitAsync(toCommit, null)
    }

    private fun report() {
        var pTime: Duration = processingTime
        if (queueSize > 0) pTime = pTime.plus(Duration.between(processingStart, Instant.now()))
        val messagesPerSecond = if (pTime.isZero) 0.0 else (processed * 1.0e9) / pTime.toNanos()

        log.info("Queue size: %,10d\tProcessed: %,10d\tMessages per second: %10.2f".format(queueSize, processed, messagesPerSecond))
    }

    override fun close() {
        runBlocking {
            log.info("Stopping...")
            commit()
            orchScope.cancel()
            orchDispatcher.cancel()
            orchExecutor.shutdownNow()
            log.info("Stopped.")
        }
    }
}
