# spring-barebones

A simple Java program to process messages from a Kafka topic using abstractions from [Spring for Apache Kafka](https://spring.io/projects/spring-kafka).


## Overview

This project is named "-barebones" because it doesn't actually create a Spring application context but rather codes directly
to the essential APIs provided by Spring for Apache Kafka. In this way, we opt-out of software machinery like the `@EnableKafka`
annotation which lets us focus on the core APIs.


## Instructions

Follow these instructions to get up and running with a Kafka broker and run the example program.

1. Use Java 17
2. Install Kafka and `kcat`:
   * ```shell
     brew install kafka
     ```
   * Note: the version I used at the time was 3.3.1_1. Check your installed version with `brew list --versions kafka`.
   * ```shell
     brew install kcat
     ```
3. Start Kafka with:
   * ```shell
     ./scripts/start-kafka.sh
     ```
4. In a new terminal, build and run the program with:
   * ```shell
     ./gradlew run
     ```
5. In a new terminal, produce some test Kafka messages with:
   * ```shell
     ./scripts/produce.sh 3
     ```
6. Look at the app logs! The app will be processing the messages.
7. Stop Kafka with:
   * ```shell
     ./scripts/stop-kafka.sh
     ```


### Wish list

General clean ups, TODOs and things I wish to implement for this project:

* DONE Remove Spring Boot and just focus on learning Spring Kafka
* DONE Try to wire up the Spring Kafka objects programmtically instead of relying on the annotations (i.e. EnableKafka and @KafkaListener) 
* DONE Can I remove the Spring app context entirely and just use the most useful parts of the 'spring-kafka' library directly? 
