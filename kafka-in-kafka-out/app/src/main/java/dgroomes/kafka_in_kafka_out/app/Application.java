package dgroomes.kafka_in_kafka_out.app;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listen to incoming messages on a Kafka topic, find the lowest alphanumeric word, and send it to another Kafka topic.
 */
public class Application {

    private static final String INPUT_TOPIC = "input-text";
    private static final Duration pollDuration = Duration.ofMillis(200);
    private static final String OUTPUT_TOPIC = "lowest-word";

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final Consumer<Void, String> consumer;
    private final Producer<Void, String> producer;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private Thread consumerThread;

    public Application(Consumer<Void, String> consumer, Producer<Void, String> producer) {
        this.consumer = consumer;
        this.producer = producer;
    }

    /**
     * (Non-blocking) Start the application
     */
    public void start() {
        active.getAndSet(true);
        consumer.subscribe(List.of(INPUT_TOPIC));
        consumerThread = new Thread(this::pollContinuously);
        consumerThread.start();
    }

    public void pollContinuously() {
        try {
            while (active.get()) {
                ConsumerRecords<Void, String> records = consumer.poll(pollDuration);
                var count = records.count();
                if (count == 0) continue;
                log.debug("Poll returned {} records", count);
                for (ConsumerRecord<Void, String> record : records) {
                    var message = record.value();

                    int sortFactor;
                    var header = record.headers().lastHeader("sort_factor");
                    if (header == null) {
                        sortFactor = 1;
                    } else {
                        sortFactor = Integer.parseInt(new String(header.value()));
                    }

                    log.trace("Got message: {}", message);
                    var lowest = lowest(message, sortFactor);
                    log.trace("Found lowest: {}", lowest);
                    send(lowest);
                    consumer.commitAsync();
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

    /**
     * Sort the words in the string (separated by space) in alphanumeric order and then get the lowest word.
     * <p>
     * This is a contrived example of a CPU-intensive operation.
     */
    private String lowest(String text, int sortFactor) {
        var words = new ArrayList<>(Arrays.asList(text.split(" ")));
        for (int i = 0; i < sortFactor; i++) {
            Collections.shuffle(words);
            Collections.sort(words);
        }

        if (words.isEmpty()) {
            return "";
        } else {
            return words.getFirst();
        }
    }

    /**
     * Send a message to the Kafka topic "lowest-word"
     */
    private void send(String msg) {
        ProducerRecord<Void, String> record = new ProducerRecord<>(OUTPUT_TOPIC, null, msg);
        Future<RecordMetadata> future = producer.send(record);
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Something went wrong while waiting for the message to be completely sent to Kafka. Shutting down the app...", e);
            System.exit(1);
        }
    }

    public void stop() throws InterruptedException {
        synchronized (active) {
            log.info("Stopping");
            if (active.get()) {
                active.getAndSet(false);
                consumer.wakeup();
                consumerThread.join();
                consumer.close();
                producer.close();
            } else {
                log.info("Already stopped");
            }
        }
    }
}
