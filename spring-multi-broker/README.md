# spring-multi-broker

A Spring Kafka application that consumes from multiple Kafka brokers.


## Overview 

In moderately complex applications there might be a need to consume from multiple Kafka clusters. While Spring Boot offers
a low-code way to consume from a single Kafka broker it does not have an obvious way to consume from multiple Kafka brokers.
The goal of this project is to find and illustrate the idiomatic way to configure and code a Spring Boot app to connect
to multiple Kafka brokers. In other words:

> What is the "Spring way" to accommodate multi-cluster Kafka infrastructure?


## Instructions

Follow these instructions to get up and running with two Kafka brokers and run the example program.

1. Pre-requisites: Java, Kafka and kcat
   * I used Java 21 installed via SDKMAN.
   * I used Kafka 3.8.0 installed via Homebrew.
   * I used kcat 1.7.0 installed via Homebrew.
   * Tip: check your HomeBrew-installed package versions with a command like the following.
   * ```shell
      brew list --versions kafka
      ```
2. Start Kafka
   * ```shell
     ./scripts/start-kafka.sh
     ```
3. Build and run the program:
   * ```shell
     ./scripts/build.sh && ./scripts/run.sh
     ```
4. Produce messages
   * In a new terminal, produce some test data to each of the "A" and "B" brokers with the following commands:
   * ```shell
     echo "hello A!" | kcat -P -b localhost:9092 -t my-messages
     ```
   * ```shell
     echo "hello B!" | kcat -P -b localhost:9192 -t my-messages
     ```
   * You should see the application react via the logs!
5. Stop all components
   * When you are done, stop the application in the other terminal.
   * Stop the Kafka brokers with the following command.
   * ```shell
     ./scripts/stop-kafka.sh
     ```


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [x] DONE Update the Kafka `server.properties` and start scripts to match the changes I made (2022-12-31) to those files in
  `utility-scripts`. 


## Reference

* [Spring for Apache Kafka: Reference Doc](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
