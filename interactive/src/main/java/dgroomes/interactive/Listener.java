package dgroomes.interactive;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class listens to incoming messages from the Kafka topic "my-messages" and is configurable with an "action" to perform
 * for each message. It has controls to stop, start and rewind and it captures some basic metrics.
 */
public class Listener {

    public static final String MY_MESSAGES_TOPIC = "my-messages";
    private static final Logger log = LoggerFactory.getLogger(Listener.class);

    private final KafkaConsumer<Void, String> consumer;

    private final Duration pollDuration = Duration.of(2, ChronoUnit.SECONDS);

    private final AtomicBoolean active = new AtomicBoolean(false);

    private Thread consumerThread;

    private final Consumer<String> action;

    /**
     * @param action execute for each message received from the Kafka topic
     */
    public Listener(Consumer<String> action) {
        this.action = action;
        Properties config = new Properties();
        config.put("group.id", "my-group");
        config.put("bootstrap.servers", "localhost:9092");
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        /*
         * Restrict the max number of records to poll so we can more easily recreate consumer lag, for educational
         * purposes. Understanding lag is a key part to designing a successful system around Kafka.
         */
        config.put("max.poll.records", 10);
        config.put("metric.reporters", Metrics.class.getName());
        consumer = new KafkaConsumer<>(config);
    }

    /**
     * Start consuming messages
     */
    public void start() {
        synchronized (active) {
            log.info("Starting");
            if (active.get()) {
                log.info("Already started");
            } else {
                active.getAndSet(true);
                consumer.subscribe(List.of(MY_MESSAGES_TOPIC));
                consumerThread = new Thread(this::pollContinuously);
                consumerThread.start();
            }
        }
    }

    private void pollContinuously() {
        try {
            while (active.get()) {
                ConsumerRecords<Void, String> records = consumer.poll(pollDuration);
                log.trace("Found {} records after poll", records.count());
                for (ConsumerRecord<Void, String> record : records) {
                    log.trace("Found record: {}", record);
                    String message = record.value();
                    action.accept(message);
                    consumer.commitSync();
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

    public void stop() throws InterruptedException {
        synchronized (active) {
            log.info("Stopping");
            if (active.get()) {
                active.getAndSet(false);
                consumer.wakeup();
                consumerThread.join();
            } else {
                log.info("Already stopped");
            }
        }
    }

    /**
     * (Blocking) Reset the Kafka offsets to the beginning
     */
    public void reset() throws InterruptedException {
        executeOperation(() -> {
            log.info("Resetting offsets");
            var partitionInfos = consumer.partitionsFor(MY_MESSAGES_TOPIC);
            var topicPartitions = partitionInfos.stream()
                    .map(it -> new TopicPartition(it.topic(), it.partition()))
                    .collect(Collectors.toList());
            consumer.seekToBeginning(topicPartitions);
        });
    }

    /**
     * (Blocking) Rewind the Kafka offsets of each TopicPartition by N spots
     */
    public void rewind(int n) throws InterruptedException {
        executeOperation(() -> {
            log.info("Rewinding offsets by {} spots for each topic partition", n);
            var partitionInfos = consumer.partitionsFor(MY_MESSAGES_TOPIC);
            for (PartitionInfo partitionInfo : partitionInfos) {
                var topicPartition = new TopicPartition(partitionInfo.topic(), partitionInfo.partition());
                var nextOffsetToRead = consumer.position(topicPartition);
                consumer.seek(topicPartition, nextOffsetToRead - 5);
            }
        });
    }

    /**
     * Print the current Kafka offsets for each topic partition
     */
    public void currentOffsets() throws InterruptedException {
        executeOperation(() -> {
            log.info("Current offsets:");
            var partitionInfos = consumer.partitionsFor(MY_MESSAGES_TOPIC);
            for (PartitionInfo partitionInfo : partitionInfos) {
                var topicPartition = new TopicPartition(partitionInfo.topic(), partitionInfo.partition());
                var currentOffset = consumer.position(topicPartition);
                log.info("topicPartition={} currentOffset={}", topicPartition.toString(), currentOffset);
            }
        });
    }

    /**
     * 1. stop the KafkaConsumer if started
     * 2. Execute an operation
     * 3. start the KafkaConsumer if was started before the operation
     */
    private void executeOperation(Runnable operation) throws InterruptedException {
        synchronized (active) {
            boolean needsStart;
            if (active.get()) {
                stop();
                needsStart = true;
            } else {
                needsStart = false;
            }
            operation.run();
            if (needsStart) {
                start();
            }
        }
    }

    /**
     * Print the current Kafka consumer lag. This will print lag for all topic-partitions.
     */
    public void lag() {
        var lagMetrics = Metrics.metricsMap.getOrDefault(Metrics.METRIC_LAG, null);

        if (lagMetrics == null) {
            log.info("Did not find a metric value for '{}'", Metrics.METRIC_LAG);
        } else {
            for (KafkaMetric topicPartitionLag : lagMetrics) {
                log.info("Kafka consumer lag ('{}'): {}", topicPartitionLag.metricName(), topicPartitionLag.metricValue());
            }
        }
    }
}
