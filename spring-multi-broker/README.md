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

1. Use Java 17
2. Install Kafka and `kcat`:
   * ```shell
     brew install kafka
     ```
   * Note: the version I used at the time was 3.3.1_1. Check your installed version with `brew list --versions kafka`.
   * ```shell
     brew install kcat
     ```
3. Start Kafka
   * Source the commands script (see [`commands.sh`](#commandssh)) and then start two Kafka brokers with the following
     commands.
   * ```shell
     . commands.sh
     ```
   * ```shell
     startKafka
     ```
4. Build and run the program:
   * ```shell
     build && run
     ```
5. Produce messages
   * In a new terminal, produce some test data to each of the "A" and "B" brokers with the following commands:
   * ```shell
     produceBrokerA
     ```
   * ```shell
     produceBrokerB
     ```
   * You should see the application react via the logs!
6. Stop all components
   * When you are done, stop the application in the other terminal.
   * Stop the Kafka brokers with the following command.
   * ```shell
     stopKafka
     ```


## `commands.sh`

Source the `commands.sh` file using `source commands.sh` which will load your shell with useful 
commands. Commands include:

  * `startKafka` start the two Kafka brokers
  * `stopKafka` stop the Kafka brokers
  * `build` build (without tests)
  * `run` run the app
  * `produceBrokerA` produce a test message to the `my-messages` Kafka topic on the "A" Kafka cluster 
  * `produceBrokerB` produce a test message to the `my-messages` Kafka topic on the "B" Kafka cluster 


## Reference

* [Spring for Apache Kafka: Reference Doc](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
