package dgroomes.kafka_playground.streams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The application runner. Initializes and starts the Kafka Streams topology. Handles shutdown.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Let's implement a basic Kafka Streams application and learn something in the process!");

        var app = new App();
        Runtime.getRuntime().addShutdownHook(new Thread("streams-wordcount-shutdown-hook") {
            @Override
            public void run() {
                log.info("Stopping the topology");
                app.stop();
            }
        });

        app.start();
    }
}
