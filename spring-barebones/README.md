# spring-barebones

A simple Java program to process messages from a Kafka topic using abstractions from [Spring for Apache Kafka](https://spring.io/projects/spring-kafka).


## Overview

This project is named "-barebones" because it doesn't actually create a Spring application context but rather codes directly
to the essential APIs provided by Spring for Apache Kafka. In this way, we opt-out of software machinery like the `@EnableKafka`
annotation which lets us focus on the core APIs.


## Instructions

Follow these instructions to get up and running with a Kafka broker and run the example program.

1. Pre-requisites: Java, Kafka and kcat
   * I used Java 21 installed via SDKMAN.
   * I used Kafka 3.7.0 installed via Homebrew.
   * I used kcat 1.7.0 installed via Homebrew.
   * Tip: check your HomeBrew-installed package versions with a command like the following.
   * ```shell
      brew list --versions kafka
      ```
2. Start Kafka with
   * ```shell
     ./scripts/start-kafka.sh
     ```
3. Build the program distribution
   * ```shell
     ./gradlew installDist
     ```
4. Run the program
   * ```shell
     ./build/install/spring-barebones/bin/spring-barebones
     ```
5. In a new terminal, produce some test Kafka messages
   * ```shell
     ./scripts/produce.sh 3
     ```
   * Now, look at the app logs. The app will be processing the messages.
6. Stop Kafka
   * ```shell
     ./scripts/stop-kafka.sh
     ```


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [x] DONE Remove Spring Boot and just focus on learning Spring Kafka
* [x] DONE Try to wire up the Spring Kafka objects programmtically instead of relying on the annotations (i.e. EnableKafka and @KafkaListener) 
* [x] DONE Can I remove the Spring app context entirely and just use the most useful parts of the 'spring-kafka' library directly? 
