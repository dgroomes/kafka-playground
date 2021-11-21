package dgroomes.interactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Application {

    private static final String CMD_START = "start";
    private static final String CMD_STOP = "stop";
    private static final String CMD_RESET = "reset";
    private static final String CMD_REWIND = "rewind";
    private static final String CMD_CURRENT_OFFSETS = "current-offsets";
    private static final String CMD_LAG = "lag";

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private Listener listener;

    /**
     * (Non-blocking) Start the application
     */
    void start() {
        listener = new Listener(msg -> {
            log.info("Got message: {}", msg);
            try {
                /*
                 * Simulate some time that it would take for processing to occur on this message. For example, an application
                 * might key off of some identifying information in the message and use it to make an API to find additional
                 * information. Then, it might be do some computation with all the data. Then, it might insert the
                 * computed data into a database. This takes time, so let's simulate the processing time.
                 *
                 * By simulating processing time we can more easily recreate Kafka consumer lag. Use the `currentOffsets`
                 * shell command while there is lag and you'll see the exact lag value in action.
                 */
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.error("Something went wrong while sleeping to simulate message processing logic.");
            }
        });
        listener.start();
    }

    /**
     * Stop the application.
     */
    void stop() throws InterruptedException {
        listener.stop();
    }

    /**
     * (Blocking) Accept input from standard input.
     */
    void acceptInput() throws IOException, InterruptedException {
        printUsage();
        var reader = new BufferedReader(new InputStreamReader(System.in));
        String command;
        while ((command = reader.readLine()) != null) {
            switch (command) {
                case CMD_START:
                    listener.start();
                    break;
                case CMD_STOP:
                    listener.stop();
                    break;
                case CMD_RESET:
                    listener.reset();
                    break;
                case CMD_REWIND:
                    listener.rewind(5);
                    break;
                case CMD_CURRENT_OFFSETS:
                    listener.currentOffsets();
                    break;
                case CMD_LAG:
                    listener.lag();
                    break;
                default:
                    if (command.isBlank()) {
                        log.warn("Nothing was entered. Please enter a supported command.");
                    } else {
                        log.warn("command '{}' not recognized", command);
                    }
                    printUsage();
                    break;
            }
        }
    }

    /**
     * Print usage information.
     */
    private void printUsage() {
        log.info("Enter '{}' to start consuming from Kafka", CMD_START);
        log.info("Enter '{}' to stop consuming from Kafka", CMD_STOP);
        log.info("Enter '{}' to reset Kafka offsets to the beginning", CMD_RESET);
        log.info("Enter '{}' to rewind Kafka offsets by a few spots", CMD_REWIND);
        log.info("Enter '{}' to get current Kafka offsets", CMD_CURRENT_OFFSETS);
        log.info("Enter '{}' to get the current Kafka consumer lag", CMD_LAG);
    }
}
