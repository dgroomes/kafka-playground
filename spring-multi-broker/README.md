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
   * I used Kafka 3.7.0 installed via Homebrew.
   * I used kcat 1.7.0 installed via Homebrew.
   * Tip: check your HomeBrew-installed package versions with a command like the following.
   * ```shell
      brew list --versions kafka
      ```
2. Start Kafka
   * Source the commands script (see [`commands.sh`](#commandssh)) and then start two Kafka brokers with the following
     commands.
   * ```shell
     . commands.sh
     ```
   * ```shell
     startKafka
     ```
3. Build and run the program:
   * ```shell
     build && run
     ```
4. Produce messages
   * In a new terminal, produce some test data to each of the "A" and "B" brokers with the following commands:
   * ```shell
     produceBrokerA
     ```
   * ```shell
     produceBrokerB
     ```
   * You should see the application react via the logs!
5. Stop all components
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


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [x] DONE Update the Kafka `server.properties` and start scripts to match the changes I made (2022-12-31) to those files in
  `utility-scripts`. 


## Reference

* [Spring for Apache Kafka: Reference Doc](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
