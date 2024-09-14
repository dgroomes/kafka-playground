package dgroomes.kafka_in_kafka_out.kafka_utils

import kotlinx.coroutines.*
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.time.toKotlinDuration

/**
 * An asynchronous Kafka consumer and message processor. Work is scheduled with coroutines.
 *
 * Messages are processed in "key order". This means that a message with a key of "xyz" will always be processed before
 * a later message with a key of "xyz". Messages with the same key are assumed to be on the same partition. The
 * algorithm is designed for high bandwidth and low latency but the implementation allocates lots of objects and I think
 * spends a lot of cycles on context switching. One of my goals with this was to explore the expressiveness of the
 * coroutines programming model. The end result I think is pretty expressive. By contrast, I can imagine an
 * implementation with more lock-free data structures, and maybe bitsets, but at the maintenance/understandability
 * cost of a higher volume of code and lower-level code.
 *
 *  - TODO Consider using a SupervisorJob so that the whole thing doesn't die if one handler execution fails.
 *  - TODO backpressure. Only poll if we're under budget.
 *  - TODO Idea: the level of sophistication is already to the max for what I'm trying to show, but an idea for an iterative
 *     upgraded example would be to give more context to an event handler; the whole set of events for the key. This
 *     would be like coalescing (?) events? Or maybe more like fusing?
 *  - TODO: Think more critically about shutdown. Ideally we want to let processing of in-flight records to have a chance
 *     to finish, but we also want to do a hard shutdown after some time.
 *  - TODO: how do consumer rebalances affect this? Pretty sure that breaks a lot.
 */
class KeyBasedAsyncConsumerWithCoroutines<KEY, PAYLOAD>(
    private val topic: String,
    pollDelay: Duration,
    private val consumer: Consumer<KEY, PAYLOAD>,
    private val handlerFn: SuspendingRecordProcessor<KEY, PAYLOAD>,
    reportingInterval: Duration = Duration.ofMinutes(1),
    commitDelay: Duration
) : HighLevelConsumer {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val orchExecutor: ExecutorService
    private val orchDispatcher: CoroutineDispatcher
    private val orchScope: CoroutineScope
    private val pollDelay: kotlin.time.Duration
    private val commitDelay: kotlin.time.Duration
    private val reportingDelay: kotlin.time.Duration
    private val tailHandlerJobByKey = mutableMapOf<KEY, Job>()
    private val tailOffsetJobByPartition = mutableMapOf<Int, Job>()
    private val nextOffsets = mutableMapOf<Int, Long>()

    init {
        val namedFactory = ThreadFactory {
            // "orch" is short for "orchestrator"
            Thread(it).apply { name = "orch-loop" }
        }
        orchExecutor = Executors.newSingleThreadExecutor(namedFactory)
        orchDispatcher = orchExecutor.asCoroutineDispatcher()
        orchScope = CoroutineScope(orchDispatcher)
        this.pollDelay = pollDelay.toKotlinDuration()
        this.commitDelay = commitDelay.toKotlinDuration()
        this.reportingDelay = reportingInterval.toKotlinDuration()
    }

    override fun start() {
        consumer.subscribe(listOf(topic))
        orchScope.launch { poll() }
        orchScope.launch { commit() }
        orchScope.launch { report() }
    }

    private suspend fun poll() {
        while (orchScope.isActive) {
            delay(pollDelay)
            val records = consumer.poll(Duration.ZERO)
            log.debug("Polled {} records", records.count())
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
                        handlerFn.process(record)
                    }

                    // Clean up the reference to the tail job, unless another job has taken its place.
                    if (tailHandlerJobByKey[key] == currentCoroutineContext().job) tailHandlerJobByKey.remove(key)
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
        }
    }

    private suspend fun commit() {
        while (orchScope.isActive) {
            delay(commitDelay)
            commitOffsets()
        }
    }

    private fun commitOffsets() {
        if (nextOffsets.isEmpty()) return

        val toCommit = nextOffsets.map { (partition, offset) ->
            TopicPartition(topic, partition) to OffsetAndMetadata(offset)
        }.toMap()

        nextOffsets.clear()

        consumer.commitAsync(toCommit) { _, exception ->
            if (exception != null) log.error("Failed to commit offsets", exception)
        }
    }

    private suspend fun report() {
        while (orchScope.isActive) {
            delay(reportingDelay)
            // TODO log total polled, total in-flight, total processed, etc.
        }
    }

    override fun close() {
        runBlocking {
            log.info("Stopping...")
            commitOffsets()
            orchScope.cancel()
            orchDispatcher.cancel()
            orchExecutor.shutdownNow()
            log.info("Stopped.")
        }
    }
}
