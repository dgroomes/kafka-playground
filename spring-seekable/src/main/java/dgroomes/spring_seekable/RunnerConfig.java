package dgroomes.spring_seekable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class RunnerConfig {

    private static final Logger log = LoggerFactory.getLogger(RunnerConfig.class);

    /**
     * The user can press the "enter" key to seek the app to the beginning of the Kafka topic
     *
     * Modeled after https://docs.spring.io/spring-kafka/docs/2.2.7.RELEASE/reference/html/#seek
     */
    @Bean
    public ApplicationRunner runner(Listener listener) {
        return args -> {
            listener.await();
            log.info("Press enter/return to seek to the beginning of the Kafka topic");
            //noinspection InfiniteLoopStatement
            while (true) {
                //noinspection ResultOfMethodCallIgnored
                System.in.read();
                listener.seekToStart();
            }
        };
    }
}
