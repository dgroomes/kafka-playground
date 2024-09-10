# connection-check

Use the Java Kafka client to check for a connection to a Kafka cluster. Sometimes, this is called a *health check*.


## Instructions

Follow these instructions to get up and running with Kafka and run the example program.

1. Pre-requisites: Java, Kafka and kcat
   * I used Java 21 installed via SDKMAN.
   * I used Kafka 3.8.0 installed via Homebrew.
   * I used kcat 1.7.0 installed via Homebrew.
   * Tip: check your HomeBrew-installed package versions with a command like the following.
   * ```shell
      brew list --versions kafka
      ```
2. Build the program distribution
   * ```shell
     ./gradlew installDist
     ```
3. Run the connection-check program
   * ```shell
     ./build/install/connection-check/bin/connection-check
     ```
   * Notice that it continuously yields a "COULD NOT CONNECT ❌" message every few because the Kafka instance is not
     running.
4. Start Kafka
   * ```shell
     ./scripts/start-kafka.sh
     ```
   * Wait for a few seconds, and notice that the connection-check program now yields a "CONNECTED ✅" message!
5. When done, stop Kafka
   * ```shell
     ./scripts/stop-kafka.sh
     ```

Altogether, the connection-check program will output something like this:

```text
./build/install/connection-check/bin/connection-check

> Task :run
15:02:11 ERROR ConnectionCheckMain - Kafka connection-check: COULD NOT CONNECT ❌
15:02:21 ERROR ConnectionCheckMain - Kafka connection-check: COULD NOT CONNECT ❌
15:02:27 INFO ConnectionCheckMain - Kafka connection-check: CONNECTED ✅
15:02:32 INFO ConnectionCheckMain - Kafka connection-check: CONNECTED ✅
15:02:42 ERROR ConnectionCheckMain - Kafka connection-check: COULD NOT CONNECT ❌
15:02:52 ERROR ConnectionCheckMain - Kafka connection-check: COULD NOT CONNECT ❌
```


## Notes

For a Java program, I think using the `AdminClient` is the most idiomatic way to check for a connection to a Kafka
cluster.

The amount of logs coming out of the Kafka client is verbose, and doubly so because this program instantiates a new
instance of the Kafka `Admin` type for each connection attempt, but this is the best I could come up with.
