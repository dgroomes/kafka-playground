# spring-headers

A basic Spring Kafka application that showcases the Spring framework behavior around Kafka message headers.

## Overview 

Kafka messages can have headers which are often leveraged to enable automatic serialization/deserialization. Spring 
Kafka (<https://spring.io/projects/spring-kafka>), in conjunction with Spring Boot, applies a significant amount of 
software machinery on top of the Java Kafka client. Among other things, this machinery adds behavior around the Kafka 
message headers. This project aims to de-mystify and illuminate that behavior. Let's learn something!    


## Instructions

Follow these instructions to get up and running with a Kafka broker and to run the example program.

1. Pre-requisites: Java, Kafka and kcat
   * I used Java 21 installed via SDKMAN.
   * I used Kafka 3.8.0 installed via Homebrew.
   * I used kcat 1.7.0 installed via Homebrew.
   * Tip: check your HomeBrew-installed package versions with a command like the following.
   * ```shell
      brew list --versions kafka
      ```
2. Start Kafka
   * Source the commands script (see [`commands.sh`](#commandssh)) and then start Kafka with the following commands.
   * ```shell
     . commands.sh
     ```
   * ```shell
     startKafka
     ```
3. Build and run the program
   * ```shell
     build && run
     ```
4. Produce a test message
   * Open a new terminal, then execute the following command to produce a test Kafka message.
   * ```shell
     produceMessageA
     ```
   * In the logs, you should see that the application read the message!
5. Stop all components
   * When you are done, stop the application in the other terminal.
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
