# spring-headers

A basic Spring Kafka application that showcases the Spring framework behavior around Kafka message headers.

---

Kafka messages can have headers which are often leveraged to enable automatic serialization/deserialization. Spring 
Kafka (<https://spring.io/projects/spring-kafka>), in conjunction with Spring Boot, applies a significant amount of 
software machinery on top of the Java Kafka client. Among other things, this machinery adds behavior around the Kafka 
message headers. This project aims to de-mystify and illuminate that behavior. Let's learn something!    

### Instructions

1. Use Java 17
2. Install Kafka and `kcat`
   * `brew install kafka`
   * `brew install kcat`
3. Start Kafka
   * Source the commands script (see [`commands.sh`](#commandssh)) and then start Kafka with the following commands.
   * `. commands.sh`
   * `startKafka`
4. Build and run the program
   * `build && run`
5. Produce some test data
   * Open a new terminal, then execute the following command to produce some test Kafka messages.
   * `produceMessageA`
   * In the logs, you should see that the application read the message!

### `commands.sh`

Source the `commands.sh` file using `source commands.sh` which will load your shell with useful 
commands. Commands include:

  * `startKafka` start Kafka
  * `stopKafka` stop Kafka
  * `build` build (without tests)
  * `run` run the app
  * `produceMessageA` produce a test message to the `my-messages` Kafka topic for the "MessageA" type 
