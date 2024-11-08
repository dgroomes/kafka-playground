package dgroomes.kafka_consumer_batch;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * See the README for more information.
 */
public class ParallelWithinSamePollConsumer implements Closeable {

    private static final Logger log = LoggerFactory.getLogger("consumer");

    @FunctionalInterface
    public interface RecordProcessor {

        void process(ConsumerRecord<String, String> record);
    }

    private final Consumer<String, String> consumer;
    private final Duration pollDuration;
    private final RecordProcessor recordProcessor;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private Thread eventThread;

    public ParallelWithinSamePollConsumer(Duration pollDuration, Consumer<String, String> consumer, RecordProcessor recordProcessor) {
        this.pollDuration = pollDuration;
        this.consumer = consumer;
        this.recordProcessor = recordProcessor;
    }

    /**
     * (Non-blocking) Start the application
     */
    public void start() {
        active.getAndSet(true);
        eventThread = Thread.ofPlatform().name("consumer").start(this::pollContinuously);
    }

    public void pollContinuously() {
        try {
            while (true) {
                if (!active.get()) {
                    log.info("The 'active' flag is false. Exiting the event loop.");
                    break;
                }

                var records = consumer.poll(pollDuration);
                var count = records.count();
                if (count == 0) {
                    continue;
                }

                log.debug("Poll received %,d records".formatted(count));

                // The semantics of a Kafka system are that the order of records matters within a partition. This gives
                // us a degree of freedom for parallel processing because we group the records by their topic-partition
                // and process each group in parallel.
                var groups = StreamSupport.stream(records.spliterator(), false)
                        .collect(Collectors.groupingBy(record ->
                                new TopicPartition(record.topic(), record.partition())))
                        .values();

                log.debug("Separated %,d groups of records".formatted(groups.size()));

                // Parallel stream 1...10 and print
                IntStream.range(1, 11)
                        .parallel()
                        .forEach(i -> {
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            System.out.println(String.valueOf(i));
                        });

                // DOES NOT actually parallelize even though I was definitely getting parallel in the "batch-consumer"....
                groups
                        .parallelStream()
                        .forEach(group -> group.forEach(recordProcessor::process));

                consumer.commitAsync();
                log.debug("Processed %,d records".formatted(count));
            }
        } catch (WakeupException e) {
            // Ignore exception if inactive because this is expected from the "stop" method
            if (active.get()) {
                log.info("rethrowing");
                throw e;
            }
        }
    }

    @Override
    public void close() {
        log.info("Stopping...");
        active.getAndSet(false);
        consumer.wakeup();
        try {
            eventThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        consumer.close();
    }
}
