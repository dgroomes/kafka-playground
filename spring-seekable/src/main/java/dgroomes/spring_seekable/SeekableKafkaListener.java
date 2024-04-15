package dgroomes.spring_seekable;

import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ConsumerSeekAware;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public abstract class SeekableKafkaListener implements ConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(SeekableKafkaListener.class);

    private final Map<TopicPartition, ConsumerSeekCallback> callbacks = new ConcurrentHashMap<>();

    private static final ThreadLocal<ConsumerSeekCallback> callbackForThread = new ThreadLocal<>();

    private final CountDownLatch isReady = new CountDownLatch(1);

    /**
     * Await for the listener to be ready
     */
    public void await() throws InterruptedException {
        log.info("Awaiting...");
        isReady.await();
        log.info("Ready");
    }

    @Override
    public void registerSeekCallback(ConsumerSeekCallback callback) {
        callbackForThread.set(callback);
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        log.debug("onPartitionsAssigned: assigning the callbacks");
        for (TopicPartition topicPartition : assignments.keySet()) {
            this.callbacks.put(topicPartition, callbackForThread.get());
        }
        isReady.countDown();
    }

    @Override
    public void onIdleContainer(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
    }

    public void seekToStart() {
        this.callbacks.forEach((tp, callback) -> {
            String topic = tp.topic();
            int partition = tp.partition();
            log.info("Seeking to the beginning of {}-{}", topic, partition);
            callback.seekToBeginning(topic, partition);
            log.info("Seeked to the beginning of {}-{}", topic, partition);
        });
    }
}
