package dgroomes.kafka_in_kafka_out.kafka_utils;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

/**
 * This synchronously processes every batch of records received via {@link Consumer#poll(Duration)}.
 */
public class SyncConsumer<KEY, PAYLOAD> implements HighLevelConsumer {

    private static final Logger log = LoggerFactory.getLogger("consumer.sync");

    private final String topic;
    private final Consumer<KEY, PAYLOAD> consumer;
    private final Duration pollDuration;
    private final RecordProcessor<KEY, PAYLOAD> recordProcessor;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private Thread eventThread;
    private final AtomicInteger processedSize = new AtomicInteger(0);

    public SyncConsumer(String topic, Duration pollDuration, Consumer<KEY, PAYLOAD> consumer, RecordProcessor<KEY, PAYLOAD> recordProcessor) {
        this.topic = topic;
        this.pollDuration = pollDuration;
        this.consumer = consumer;
        this.recordProcessor = recordProcessor;
    }

    /**
     * (Non-blocking) Start the application
     */
    public void start() {
        active.getAndSet(true);
        consumer.subscribe(List.of(topic));
        eventThread = new Thread(this::pollContinuously);
        eventThread.start();
    }

    public void pollContinuously() {
        try {
            while (true) {
                if (!active.get()) {
                    log.info("The 'active' flag is false. Exiting the event loop.");
                    break;
                }

                var records = consumer.poll(pollDuration);
                if (records.count() > 0) {
                    log.debug("Poll received %,d records".formatted(records.count()));
                }

                // TODO messages should be processed in order on each partition because that's the usual semantics of
                //  Kafka. We don't have a reason to break that convention here.
                StreamSupport.stream(records.spliterator(), true).forEach(record -> {
                    try {
                        recordProcessor.process(record);
                    } catch (Exception e) {
                        log.error("Something went wrong while processing a record. You need to decide how to handle this.", e);
                    }
                });

                consumer.commitAsync();
                processedSize.getAndAdd(records.count());
                if (records.count() > 0) {
                    log.debug("Processed %,d records".formatted(processedSize.get()));
                }
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
        // TODO revisit this.
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
