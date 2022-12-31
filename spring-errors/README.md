# spring-errors

A basic Spring Kafka application that showcases the Spring framework features and behavior 
around Kafka error handling.


## Overview

Spring for Apache Kafka (<https://spring.io/projects/spring-kafka>) has a significant amount of software machinery around error 
handling ([relevant section in docs](https://docs.spring.io/spring-kafka/reference/html/#annotation-error-handling)).
This project aims to de-mystify and illuminate it. Let's learn something!


## Instructions

Follow these instructions to get up and running with a Kafka broker and the example program.

1. Use Java 17
2. Install Kafka and `kcat`:
    * ```shell
      brew install kafka
      ```
    * Note: the version I used at the time was 3.3.1_1. Check your installed version with `brew list --versions kafka`.
    * ```shell
      brew install kcat
      ```
3. Running the application and the test cases depend on a locally running Kafka instance. Use the `startKafka` and 
   `stopKafka` commands (see [`commands.sh`](#commandssh)) to run Kafka. Specifically, use the following commands to
   source the `commands.sh` file and then start a Kafka broker.
    * ```shell
      . commands.sh
      ```
    * ```shell
      startKafka
      ```
4. In a new terminal, build and run the program with:
    * ```shell
      build && run
      ```
5. In a new terminal, produce some test data with:
    * ```shell
      produceValidMessage
      ```
    * You should see the application react with new logs.
6. In a new terminal, produce some test data with:
    * ```shell
      produceInvalidMessage
      ```
    * You should see the application react with new logs.
7. Consume the dead letter topic with:
    * ```shell
      consumeDeadLetterTopic
      ```
    * You should see all the "invalid" messages there. As new "invalid" messages are received by the app, they will be
      forward to this topic.
8. Stop all components
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
