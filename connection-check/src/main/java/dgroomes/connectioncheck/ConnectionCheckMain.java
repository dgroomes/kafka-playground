package dgroomes.connectioncheck;

import org.apache.kafka.clients.admin.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.*;

/**
 * Use the Java Kafka client to check for a connection to a Kafka cluster. Sometimes, this is called a *health check*.
 */
public class ConnectionCheckMain {

    private static final Logger log = LoggerFactory.getLogger(ConnectionCheckMain.class);

    public static void main(String[] args) throws InterruptedException {
        var kafkaHost = "localhost:9092";

        for (int i = 0; i < 10; i++, Thread.sleep(5_000)) {
            if (connect(kafkaHost)) {
                log.info("Kafka connection-check: CONNECTED ✅");
            } else {
                log.error("Kafka connection-check: COULD NOT CONNECT ❌");
            }
        }
    }

    /**
     * Can the Kafka cluster be connected to?
     *
     * @param host the hostname of the Kafka cluster that this method will attempt to connect to
     * @return true if a connection was established
     */
    private static boolean connect(String host) {
        var properties = new Properties();
        properties.put("bootstrap.servers", host);

        // The Java Kafka client library does not offer a "cluster connection check" mechanism so we can invent our own
        // by using the method "org.apache.kafka.clients.admin.Admin.describeCluster()".
        //
        // This is a blocking method that returns only after it can successfully connect to the Kafka cluster. This is
        // not documented behavior, but perhaps it should be obvious? To me, it's not obvious and I would appreciate
        // more help both in terms of official Apache Kafka Java client code samples and in the client source code,
        // where with some intentionally designed, edited, and documented code I might be able to figure out how I
        // am meant to use the client as an end-user.
        Future<Void> checkConnection = CompletableFuture.supplyAsync(() -> {
            try (Admin kafkaAdmin = Admin.create(properties)) {
                kafkaAdmin.describeCluster();
                log.trace("The 'describeCluster' method completed. We can infer that it was able to make a connection to the Kafka cluster.");
                return null;
            }
        });

        try {
            checkConnection.get(5, TimeUnit.SECONDS);
            return true;
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            log.trace("The 'describeCluster' method never completed after some time. We can infer that it was not able to make a connection to the Kafka cluster");
            return false;
        }
    }
}
