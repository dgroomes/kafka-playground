package dgroomes.kafka_consumer_with_coroutines

import kotlinx.coroutines.*
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.toKotlinDuration

/**
 * See the README for more information.
 */
class KeyBasedAsyncConsumerWithCoroutines(
    private val topic: String,
    pollDelay: Duration,
    private val consumer: Consumer<String, String>,
    private val handlerFn: RecordProcessor,
    commitDelay: Duration
) : Closeable {

    fun interface RecordProcessor {
        suspend fun process(record: ConsumerRecord<String, String>)
    }

    private val log = LoggerFactory.getLogger("consumer.coroutines")
    private val orchExecutor: ExecutorService
    private val orchDispatcher: CoroutineDispatcher
    private val orchScope: CoroutineScope
    private val pollDelay: kotlin.time.Duration
    private val commitDelay: kotlin.time.Duration
    private val tailHandlerJobByKey = mutableMapOf<String, Job>()
    private val tailOffsetJobByPartition = mutableMapOf<Int, Job>()
    private val nextOffsets = mutableMapOf<Int, Long>()
    private var queueSize = 0
    private val queueDesiredMax = 100

    init {
        // "orch" is short for "orchestrator"
        val namedFactory = Thread.ofPlatform().name("consumer-orch").factory()
        orchExecutor = Executors.newSingleThreadExecutor(namedFactory)
        orchDispatcher = orchExecutor.asCoroutineDispatcher()
        orchScope = CoroutineScope(orchDispatcher)
        this.pollDelay = pollDelay.toKotlinDuration()
        this.commitDelay = commitDelay.toKotlinDuration()
    }

    fun start() {
        orchScope.launch { runEvery(::poll, pollDelay) }
        orchScope.launch { runEvery(::commit, commitDelay) }
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
                        handlerFn.process(record)
                    }

                    // Clean up the reference to the tail job, unless another job has taken its place.
                    if (tailHandlerJobByKey[key] == currentCoroutineContext().job) tailHandlerJobByKey.remove(key)

                    queueSize--
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
