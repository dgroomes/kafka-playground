# spring-errors

A basic Spring Kafka application that showcases the Spring framework features and behavior 
around Kafka error handling.


## Overview

Spring for Apache Kafka (<https://spring.io/projects/spring-kafka>) has a significant amount of software machinery around error 
handling ([relevant section in docs](https://docs.spring.io/spring-kafka/reference/html/#annotation-error-handling)).
This project aims to de-mystify and illuminate it. Let's learn something!


## Instructions

Follow these instructions to get up and running with a Kafka broker and the example program.

1. Pre-requisites: Java, Kafka and kcat
    * I used Java 21 installed via SDKMAN.
    * I used Kafka 3.8.0 installed via Homebrew.
    * I used kcat 1.7.0 installed via Homebrew.
    * Tip: check your HomeBrew-installed package versions with a command like the following.
    * ```shell
      brew list --versions kafka
      ```
2. Running the application and the test cases depend on a locally running Kafka instance. Use the `startKafka` and 
   `stopKafka` commands (see [`commands.sh`](#commandssh)) to run Kafka. Specifically, use the following commands to
   source the `commands.sh` file and then start a Kafka broker.
    * ```shell
      . commands.sh
      ```
    * ```shell
      startKafka
      ```
3. In a new terminal, build and run the program with:
    * ```shell
      build && run
      ```
4. In a new terminal, produce some test data with:
    * ```shell
      produceValidMessage
      ```
    * You should see the application react with new logs.
5. In a new terminal, produce some test data with:
    * ```shell
      produceInvalidMessage
      ```
    * You should see the application react with new logs.
6. Consume the dead letter topic with:
    * ```shell
      consumeDeadLetterTopic
      ```
    * You should see all the "invalid" messages there. As new "invalid" messages are received by the app, they will be
      forwarded to this topic.
7. Stop all components
    * When you are done, stop the dead letter topic Kafka consumer.
    * Stop the application.
    * Finally, stop the Kafka broker with the following command.
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
  * `produceValidMessage` produce a test JSON message to the `my-messages` Kafka topic 
  * `produceInvalidMessage` produce a test JSON message to the `my-messages` Kafka topic that has a field of the wrong type 
    JSON that the app is expecting 
  * `consumeDeadLetterTopic` consume from the Kafka topic


## Commentary

Warning: here is some editorialization! Is Spring for Apache Kafka fundamentally oriented to "one-message-at-a-time" processing?
The framework has a great configuration story for apps with a single consumer (there is exactly one 
`spring.kafka.consmer` config block), for a single cluster, and for single (non-batch) message processing flows. There 
is a sophisticated retry/recovery feature set. But it is restricted to the non-batch style. See this comment:

> A retry adapter is not provided for any of the batch message listeners, because the framework has no knowledge of
> where in a batch the failure occurred. If you need retry capabilities when you use a batch listener, we recommend that
> you use a RetryTemplate within the listener itself.
> -- <cite>https://docs.spring.io/spring-kafka/docs/2.5.1.RELEASE/reference/html/#retrying-deliveries</cite>  


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [x] DONE Upgrade to Spring Boot 3.x and in so doing, upgrade to a later version of Spring for Apache Kafka.
  * There is a Spring Boot 3.x migration guide [here](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide).
    One of the main things that stand out to me is to use the "properties migration" dependency
  * What is the migration story of Spring for Apache Kafka? Update, well there isn't a change history entry for 3.0 but
    there is [one for 2.9](https://docs.spring.io/spring-kafka/reference/html/#migration) and I'm hopeful it will
    explain some of the breaking changes. Update: well it didn't totally help but I was able to easily find that the
    `SeektoCurrentErrorHandler` was superseded by the `DefaultErrorHandler` with a simple `Cmd + F` search. Nice! Also I
    there is another nice [migration guide about the deprecated `ErrorHandler` interface](https://docs.spring.io/spring-kafka/reference/html/#migrating-legacy-eh).
