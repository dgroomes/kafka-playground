# spring-multi-broker

A Spring Kafka application that consumes from multiple Kafka brokers.

---

In moderately complex applications there might be a need to consume from multiple Kafka clusters. While Spring Boot offers
a low-code way to consume from a single Kafka broker it does not have an obvious way to consume from multiple Kafka brokers.
The goal of this project is to find and illustrate the idiomatic way to configure and code a Spring Boot app to connect
to multiple Kafka brokers. In other words:

> What is the "Spring way" to accommodate multi-cluster Kafka infrastructure?

## Instructions

1. Use Java 11
1. Install Kafka and `kcat`:
   * `brew install kafka`
   * `brew install kcat`
1. Start Kafka
   *  Read [`commands.sh`](#commandssh) and then execute the following command to start the Kafka broker:
   * `startKafka`
1. Build and run the program:
   * `build && run`
1. Produce messages
   * In a new terminal, produce some test data to each of the "A" and "B" brokers with the following commands:
   * `produceBrokerA`
   * `produceBrokerB`
   * You should see the application react via the logs!
1. Stop all components
   * When you are done, stop the application in the other terminal.
   * Stop the Kafka brokers with the following command:
   * `stopKafka`

## `commands.sh`

Source the `commands.sh` file using `source commands.sh` which will load your shell with useful 
commands. Commands include:

  * `startKafka` start Kafka and Zookeeper
  * `stopKafka` stop Kafka and Zookeeper
  * `build` build (without tests)
  * `run` run the app
  * `produceBrokerA` produce a test message to the `my-messages` Kafka topic on the "A" Kafka cluster 
  * `produceBrokerB` produce a test message to the `my-messages` Kafka topic on the "B" Kafka cluster 

## Reference

* [Spring for Apache Kafka: Reference Doc](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
