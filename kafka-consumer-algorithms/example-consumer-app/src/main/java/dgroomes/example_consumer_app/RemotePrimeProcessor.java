package dgroomes.example_consumer_app;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;

/**
 * This is a variation of {@link PrimeProcessor} that instead computes prime numbers by delegating to a remote
 * web service (e.g. an HTTP API). This is an interesting difference because the workload is no longer locally
 * CPU-intensive. In other words, our natural bottleneck is IO and not CPU. How does this change the performance
 * characteristics of the consumer?
 * <p>
 * The remote call is simulated/faked because I don't want to bother actually implementing the service. We'll use three
 * broad groups:
 * <ol>
 *     <li>"Small". Computing the 10,000th prime number or lower will be very quick at 2ms</li>
 *     <li>"Medium". Computing the 100,000th prime number or lower will take 50ms</li>
 *     <li>"Large". Computing anything larger will take 1 second</li>
 * </ol>
 */
public class RemotePrimeProcessor {

    private final KafkaProducer<String, String> producer;
    private final String outputTopic;

    public RemotePrimeProcessor(KafkaProducer<String, String> producer, String outputTopic) {
        this.producer = producer;
        this.outputTopic = outputTopic;
    }

    public void process(ConsumerRecord<String, String> record) {
        int nth = Integer.parseInt(record.value());

        Duration sleep;
        int prime;
        if (nth <= 10_000) {
            sleep = Duration.ofMillis(2);
            prime = 104_729;
        } else if (nth <= 100_000) {
            sleep = Duration.ofMillis(50);
            prime = 1_299_709;
        } else {
            sleep = Duration.ofSeconds(1);
            prime = 15_485_863;
        }

        try {
            Thread.sleep(sleep.toMillis());
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e1);
        }

        var msg = String.format("The %,d prime number is %,d (faked value)", nth, prime);
        var outRecord = new ProducerRecord<String, String>(outputTopic, record.key(), msg);
        try {
            producer.send(outRecord).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send record", e);
        }
    }
}
