# streams

A basic [Kafka Streams](https://kafka.apache.org/documentation/streams/) application.

## Description

This demo app illuminates the threading model of Kafka Streams by sleeping for each input message. For example, if ten messages are
input, and the Kafka Streams topology is bound by one thread, then it will take ten seconds to process the messages. By
contrast, if the input Kafka topic has five partitions and the Kafka Streams app is configured with five threads, then
it should take as little as two seconds to process the messages! Experiment with different configurations of the input
topic, Kafka Streams topology operations, and Kafka Streams configurations.   

## Instructions

1. Use Java 17
1. Install Kafka and `kcat`:
    * `brew install kafka`
    * `brew install kcat`   
1. Start Kafka
   *  Read [`commands.sh`](#commandssh) and then execute the following command to start the Kafka broker:
   * `startKafka`
1. Create the topics
   * Open a new terminal and create the input, intermediate, and output Kafka topics with the following command:
   * `createTopics`
1. Build and run the program:
   * `build && run`
1. Produce and consume messages
   * In a new terminal, start a consumer process which will eventually receive messages on the output Kafka topic. Use
     the following command:
   * `consume`
   * In a new terminal, produce some test data with the following command:
   * `produce`
   * You should see some data in your consumer!
1. Produce even more messages:
   * `produce 10`
1. Continue to experiment!
1. Stop all components
   * When you are done, stop the Kafka consumer in the other terminal.
   * Stop the application in the other terminal.
   * Finally, stop the Kafka broker with the following command:
   * `stopKafka`

### `commands.sh`

Source the `commands.sh` file using `source commands.sh` which will load your shell with useful 
commands. Commands include: `build`, `startKafka` `run`, `consume` etc. See the contents of the file for more.

### Wish List

Items I wish to implement for this project:

  * DONE Implement some tests
  * DONE (Answer: it's what happens when you rely on auto topic creation. The app has to stumble with the non-existing
    topics for a while and then creates them. A bit awkward in my opinion). Why, when starting the app, does it log a
    few hundred warning logs like this:
    ```
    00:23:45 [streams-wordcount-ec294eef-3f5a-401b-8b69-45084bc07506-StreamThread-10] WARN org.apache.kafka.clients.NetworkClient - [Consumer clientId=streams-wordcount-ec294eef-3f5a-401b-8b69-45084bc07506-StreamThread-10-consumer, groupId=streams-wordcount] Error while fetching metadata with correlation id 106 : {streams-wordcount-KSTREAM-AGGREGATE-STATE-STORE-0000000006-repartition=UNKNOWN_TOPIC_OR_PARTITION}
    ```
    Was it always like this? Is this normal? Is the out-of-the-box Kafka Streams operational experience always full of
    verbose warning logs? Is this a KRaft issue?
  * DONE (It turns out this is a spurious message. See https://github.com/apache/kafka/pull/10342#discussion_r599057582) Deal with this shutdown error
    ```
    ERROR org.apache.kafka.streams.processor.internals.StateDirectory - Some task directories still locked while closing state, this indicates unclean shutdown: {}
    ```
  * Is there an idiomatic way to figure out the intermediate/internal Kafka Streams topic names without actually running
    the app and printing the topology? Is there something like a dry-run option? I want to know topic names, and then
    create them before running the app. I do not want to rely an auto topic creation. I want *intentionality* with the
    application in a similar way I don't use Hibernate to create SQL tables automatically. 

## Reference

* [Apache Kafka: Quick Start *Demo Application*](https://kafka.apache.org/25/documentation/streams/quickstart)
  * This project is adapted, in part, by the quick start
* [Apache Kafka Streams: *Testing Kafka Streams*](https://kafka.apache.org/28/documentation/streams/developer-guide/testing.html)
