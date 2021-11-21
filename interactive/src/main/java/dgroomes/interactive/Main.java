package dgroomes.interactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The application runner
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        log.info("Let's learn and experiment!");
        var app = new Application();
        app.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log.info("Stopping the app");
                app.stop();
            } catch (InterruptedException e) {
                throw new RuntimeException("Shutdown thread was interrupted. Failed to stop the app.", e);
            }
        }));

        app.acceptInput();
    }
}
