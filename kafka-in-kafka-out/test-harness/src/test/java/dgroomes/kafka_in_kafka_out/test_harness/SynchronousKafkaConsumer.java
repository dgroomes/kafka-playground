package dgroomes.kafka_in_kafka_out.test_harness;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This is a super simple object to execute a Kafka consumer. In fact, this is an idiomatic example because it comes straight
 * from the official Kafka JavaDocs: http://kafka.apache.org/21/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#multithreaded
 * <p>
 * I have struggled so much while learning the threading model of the Java KafkaConsumer library, part because I'm not
 * an expert in Java concurrency, in part because the internals of the KafkaConsumer class are a bit gnarly in my opinion
 * (I'm looking at you Fetcher...), and in part because there don't exist many highlighted examples out in the wild of
 * using the Java KafkaConsumer directly. Instead, most examples are using it via a framework like Spring Kafka, Apache Spark,
 * Kafka Streams, etc. And most other examples kick off a standalone Thread to instantiate the KafkaConsumer object and
 * continuously poll it. I'm interested to make the KafkaConsumer in the main thread and poll it from the main thread.
 * But alas, this just isn't meant to be. JUnit is what I was trying to use to do this in a test-driven way but JUnit
 * uses multiple threads, like worker threads and initialization threads, so I couldn't use that easily. So instead I'm giving
 * up and just using the "Apache Kafka Java KafkaConsumer super simple consumer" that I've found in the JavaDocs.
 */
public class SynchronousKafkaConsumer implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(SynchronousKafkaConsumer.class);

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ArrayBlockingQueue<ConsumerRecord<Void, String>> queue = new ArrayBlockingQueue<>(10);
    private final String topic;
    private final Thread consumerThread;
    private final Duration pollTimeout;
    private final Duration takeTimeout;
    private final KafkaConsumer<Void, String> consumer;

    /**
     * Initialize an underlying KafkaConsumer object and start a thread to continually poll for messages.
     *
     * @param config        config to use to instnantiate the KafkaConsumer
     * @param topic         the topic to listen to
     * @param pollTimeout the timeout, in milliseconds, to wait for messages.
     */
    public SynchronousKafkaConsumer(Properties config, String topic, Duration pollTimeout, Duration takeTimeout) {
        this.topic = topic;
        this.pollTimeout = pollTimeout;
        this.takeTimeout = takeTimeout;
        this.consumer = new KafkaConsumer<>(config);
        consumerThread = new Thread(() -> {
            startConsumerAtEnd();
            continuouslyPoll();
        });
        consumerThread.start();
    }

    private void continuouslyPoll() {
        try {
            while (!closed.get()) {
                ConsumerRecords<Void, String> records = consumer.poll(pollTimeout);
                for (ConsumerRecord<Void, String> record : records) {
                    log.debug("Found a record: {}", record);
                    queue.put(record);
                }
            }
        } catch (WakeupException e) {
            // Ignore exception if closing else re-throw
            if (!closed.get()) {
                throw e;
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("The continuous poll loop was interrupted", e);
        } finally {
            consumer.close();
        }
    }

    /**
     * Assign the consumer to the topic partitions and seek to the end of those topic partitions.
     * <p>
     * It's important that these operations are executed in the same thread that all "poll" invocations are executed in.
     * The KafkaConsumer detects if more than one thread are executing methods on it and will throw a {@link java.util.ConcurrentModificationException}.
     */
    private void startConsumerAtEnd() {
        var partitionInfos = consumer.partitionsFor(topic);
        var topicPartitions = partitionInfos.stream()
                .map(it -> new TopicPartition(it.topic(), it.partition()))
                .collect(Collectors.toList());
        consumer.assign(topicPartitions);
        consumer.seekToEnd(topicPartitions);
    }


    /**
     * Take the next record on the topic.
     * <p>
     * This is a stateful operation. It works by taking the value from this object's internal blocking queue if it exists.
     * If an element does not exist, then it will block for a configured timeout. During this time, a separate consuming
     * thread is continually polling the Kafka broker for new messages. If a message is received before the wait timeout
     * is up, then this method will return a message. Else, it will throw an exception.
     */
    public ConsumerRecord<Void, String> take() {
        try {
            var found = queue.poll(this.takeTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (found == null) {
                throw new IllegalStateException("The take operation timed out");
            }
            return found;
        } catch (InterruptedException e) {
            throw new IllegalStateException("The take operation was interrupted", e);
        }
    }

    /**
     * Shutdown the consumer. This is safe to call from a separate thread.
     */
    @Override
    public void close() {
        closed.set(true);
        consumer.wakeup();
        try {
            consumerThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        consumer.close();
    }
}
