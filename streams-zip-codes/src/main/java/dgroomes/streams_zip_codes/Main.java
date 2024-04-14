package dgroomes.streams_zip_codes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The application runner. Initializes and starts the Kafka Streams topology. Handles shutdown.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("This is an intermediate Kafka Streams application to aggregate ZIP code data!");

        var topology = new App();
        Runtime.getRuntime().addShutdownHook(new Thread("streams-wordcount-shutdown-hook") {
            @Override
            public void run() {
                log.info("Stopping the topology");
                topology.stop();
            }
        });

        topology.start();
    }
}
