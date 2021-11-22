# spring-barebones

A simple Java program to process messages from a Kafka topic using abstractions from [Spring for Apache Kafka](https://spring.io/projects/spring-kafka).

---

### Description

This project is named "-barebones" because it doesn't actually create a Spring application context but rather codes directly
to the essential APIs provided by Spring for Apache Kafka. In this way, we opt-out of software machinery like the `@EnableKafka`
annotation which lets us focus on the core APIs.

### Instructions

1. Use Java 17
1. Install Kafka and `kcat`:
   * `brew install kafka`
   * `brew install kcat`
1. Start Kafka with:
   * `./scripts/start-kafka.sh`
1. In a new terminal, build and run the program with:
   * `./gradlew run`
1. In a new terminal, produce some test Kafka messages with:
   * `./scripts/produce.sh 3`
1. Look at the app logs! The app will be processing the messages.
1. Stop Kafka with:
   * `./scripts/stop-kafka.sh`

### Wish list

General clean ups, TODOs and things I wish to implement for this project:

* DONE Remove Spring Boot and just focus on learning Spring Kafka
* DONE Try to wire up the Spring Kafka objects programmtically instead of relying on the annotations (i.e. EnableKafka and @KafkaListener) 
* DONE Can I remove the Spring app context entirely and just use the most useful parts of the 'spring-kafka' library directly? 
