# streams

A basic [Kafka Streams](https://kafka.apache.org/documentation/streams/) application.


## Overview

This demo app illuminates the threading model of Kafka Streams by sleeping for each input message. For example, if ten messages are
input, and the Kafka Streams topology is bound by one thread, then it will take ten seconds to process the messages. By
contrast, if the input Kafka topic has five partitions and the Kafka Streams app is configured with five threads, then
it should take as little as two seconds to process the messages! Experiment with different configurations of the input
topic, Kafka Streams topology operations, and Kafka Streams configurations.   


## Instructions

Follow these instructions to get up and running with a Kafka broker and an example streams program.

1. Pre-requisites: Java, Kafka and kcat
    * I used Java 21 installed via SDKMAN.
    * I used Kafka 3.7.0 installed via Homebrew.
    * I used kcat 1.7.0 installed via Homebrew.
    * Tip: check your HomeBrew-installed package versions with a command like the following.
    * ```shell
      brew list --versions kafka
      ```
2. Start Kafka
   * Read [`commands.sh`](#commandssh) and then use the following commands to source the commands file and then start
     Kafka.
   * ```shell
     . commands.sh
     ```
   * ```shell
     startKafka
     ```
   * **Warning**: Between executions of the program, you may need to delete the stateful data files that Kafka Streams
     uses. The `command.sh` script defines a `cleanState` function that will do this for you. You can recognize this
     situation if you get an error that looks like this:

     > org.apache.kafka.streams.errors.TaskCorruptedException: Tasks [2_1] are corrupted and hence needs to be re-initialized

     Stop the Kafka broker if it is already running. Use the `cleanState` command to clean up the stateful data files. And
     then you may start the Kafka broker again.
3. Create the topics
   * Open a new terminal and create the input, intermediate, and output Kafka topics with the following command:
   * ```shell
     createTopics
     ```
4. Build and run the program:
   * ```shell
     build && run
     ```
5. Produce and consume messages
   * In a new terminal, start a consumer process which will eventually receive messages on the output Kafka topic. Use
     the following command:
   * ```shell
     consume
     ```
   * In a new terminal, produce some test data with the following command:
   * ```shell
     produce
     ```
   * You should see some data in your consumer!
6. Produce even more messages:
   * ```shell
     produce 10
     ```
7. Continue to experiment!
8. Stop all components
   * When you are done, stop the Kafka consumer in the other terminal.
   * Stop the application in the other terminal.
   * Finally, stop the Kafka broker with the following command:
   * ```shell
     stopKafka
     ```
9. Run the unit tests
   * This project also defines unit tests that exercise our Java source code using an in-process test harness that is an
     official part of the Kafka Java libraries. This is a nice way to test our code because it does not run a real Kafka
     broker and so it executes quickly, and it does not leave behind stateful data files that need to be cleaned up. On
     the other hand, a Kafka Streams application engages so many moving parts that you should also consider integration/functional
     tests that engage a real Kafka broker. Run the tests with the following command.
   * ```shell
     ./gradlew test
     ```


## `commands.sh`

Source the `commands.sh` file using `source commands.sh` which will load your shell with useful 
commands. Commands include: `build`, `startKafka` `run`, `consume` etc. See the contents of the file for more.


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [ ] Is there an idiomatic way to figure out the intermediate/internal Kafka Streams topic names without actually running
  the app and printing the topology? Is there something like a dry-run option? I want to know topic names, and then
  create them before running the app. I do not want to rely on auto topic creation. I want *intentionality* with the
  application in a similar way I don't use Hibernate to create SQL tables automatically.
* [ ] OBSOLETE (This went away with later upgrades) Resolve this warning message. I think this is new since Kafka 3.x
   * > WARN org.apache.kafka.streams.internals.metrics.ClientMetrics - Error while loading kafka-streams-version.properties


### Finished Wish List Items

These items were either completed or skipped.

* [x] DONE Implement some tests
* [x] DONE (Answer: it's what happens when you rely on auto topic creation. The app has to stumble with the non-existing
  topics for a while and then creates them. A bit awkward in my opinion). Why, when starting the app, does it log a
  few hundred warning logs like this:
  * > 00:23:45 [streams-wordcount-ec294eef-3f5a-401b-8b69-45084bc07506-StreamThread-10] WARN org.apache.kafka.clients.NetworkClient - [Consumer clientId=streams-wordcount-ec294eef-3f5a-401b-8b69-45084bc07506-StreamThread-10-consumer, groupId=streams-wordcount] Error while fetching metadata with correlation id 106 : {streams-wordcount-KSTREAM-AGGREGATE-STATE-STORE-0000000006-repartition=UNKNOWN_TOPIC_OR_PARTITION}

  Was it always like this? Is this normal? Is the out-of-the-box Kafka Streams operational experience always full of
  verbose warning logs? Is this a KRaft issue?
* [x] DONE (It turns out this is a spurious message. See https://github.com/apache/kafka/pull/10342#discussion_r599057582) Deal with this shutdown error
  * > ERROR org.apache.kafka.streams.processor.internals.StateDirectory - Some task directories still locked while closing state, this indicates unclean shutdown: {}


## Reference

* [Apache Kafka docs: *Run Kafka Streams Demo Application*](https://kafka.apache.org/33/documentation/streams/quickstart)
  * The `kafka-playground/streams` project is adapted, in part, by this quick start project.
* [Apache Kafka Streams: *Testing Kafka Streams*](https://kafka.apache.org/33/documentation/streams/developer-guide/testing.html)
