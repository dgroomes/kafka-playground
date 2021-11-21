# spring-headers

A basic Spring Kafka application that showcases the Spring framework behavior around Kafka message headers.

---

Kafka messages can have headers which are often leveraged to enable automatic serialization/deserialization. Spring 
Kafka (<https://spring.io/projects/spring-kafka>), in conjunction with Spring Boot, applies a significant amount of 
software machinery on top of the Java Kafka client. Among other things, this machinery adds behavior around the Kafka 
message headers. This project aims to de-mystify and illuminate that behavior. Let's learn something!    

### Instructions

* Use Java 11
* Install Kafka and `kcat`:
  * `brew install kafka`
  * `brew install kcat`
* Running the application and the test cases depend on a locally running Kafka instance. Use the `startKafka` and 
  `stopKafka` commands (see [`commands.sh`](#commandssh)) to run Kafka.
* In a new terminal, build and run the program with `build && run`
* In a new terminal, produce some test data with `produceMessageA`. You should see the application react with new logs.

### `commands.sh`

Source the `commands.sh` file using `source commands.sh` which will load your shell with useful 
commands. Commands include:

  * `startKafka` start Kafka
  * `stopKafka` stop Kafka
  * `build` build (without tests)
  * `run` run the app
  * `produceMessageA` produce a test message to the `my-messages` Kafka topic for the "MessageA" type 
