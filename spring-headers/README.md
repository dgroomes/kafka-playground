# spring-headers

A basic Spring Kafka application that showcases the Spring framework behavior around Kafka message headers.

## Overview 

Kafka messages can have headers which are often leveraged to enable automatic serialization/deserialization. Spring 
Kafka (<https://spring.io/projects/spring-kafka>), in conjunction with Spring Boot, applies a significant amount of 
software machinery on top of the Java Kafka client. Among other things, this machinery adds behavior around the Kafka 
message headers. This project aims to de-mystify and illuminate that behavior. Let's learn something!    


## Instructions

Follow these instructions to get up and running with a Kafka broker and to run the example program.

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
   * Source the commands script (see [`commands.sh`](#commandssh)) and then start Kafka with the following commands.
   * ```shell
     . commands.sh
     ```
   * ```shell
     startKafka
     ```
4. Build and run the program
   * ```shell
     build && run
     ```
5. Produce some test data
   * Open a new terminal, then execute the following command to produce some test Kafka messages.
   * ```shell
     produceMessageA
     ```
   * In the logs, you should see that the application read the message!
6. Stop all components
   * When you are done, stop the Kafka the application in the other terminal.
   * Stop the Kafka broker with the following command.
   * ```shell
      stopKafka
      ```


## `commands.sh`

Source the `commands.sh` file using `source commands.sh` which will load your shell with useful 
commands. Commands include:

  * `startKafka` start Kafka
  * `stopKafka` stop Kafka
  * `build` build (without tests)
  * `run` run the app
  * `produceMessageA` produce a test message to the `my-messages` Kafka topic for the "MessageA" type 
