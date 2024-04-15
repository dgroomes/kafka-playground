# spring-seekable

A basic Spring Kafka application with a "seekable" Kafka listener.


## Overview

In essence, the Kafka listener base class has operations to seek to the beginning or end (not implemented) of the 
Kafka topic. This is a useful feature when executing tests or when needing to replay all Kafka 
messages from the beginning of a Kafka topic.


### Instructions

Follow these instructions to get up and running with a Kafka broker and to run the the example program.

1. Pre-requisites: Java, Kafka and kcat
    * I used Java 21 installed via SDKMAN.
    * I used Kafka 3.7.0 installed via Homebrew.
    * I used kcat 1.7.0 installed via Homebrew.
    * Tip: check your HomeBrew-installed package versions with a command like the following.
    * ```shell
      brew list --versions kafka
      ```
2. Start Kafka
   * Source the commands file (see [`commands.sh`](#commandssh)) and start a Kafka broker with the following commands.
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
4. Produce some test records
   * ```shell
     produce
     ```
   * You should see the application react with new logs.
5. Produce many more records
   * ```shell
     produce 10
     ```
6. Seek the consumer to the beginning
   * In the application terminal, press "enter" to seek the consumer to the beginning of the topic. It should replay all 
     the messages on the topic.
7. Stop all components
    * When you are done, stop the application.
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
  * `runTests` execute the tests
  * `consume` consume from the `my-messages` Kafka topic
  * `produce` produce a test message to the `my-messages` Kafka topic 
  * `current-offsets` get current Kafka topic offsets for the `my-group` group 


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [ ] Fix the tests. Consider using the `kafka-in-kafka-out` as an example codebase that has (hopefully?) figured out how
  to write automated tests against a local Kafka cluster. And, consider just deleting the test.
* [x] DONE (Fixed with the upgrade to Spring Boot 3) Fix the logging system. I'm using slf4j-simple as the logging backend which doesn't support terminal colors. I
  want to use Logback. I need to use the version of Logback that works with SLF4J 2. Should be easy, but haven't tried
  it before.  


### Finished Wish List Items

These items were either completed or skipped.

* [x] DONE (wow, it was my bad all along! i never cleared the queue). Why doesn't this work? It appears to seek to the beginning but no messages get printed out that indicate any
  messages were actually re-processed.
* [x] OBSOLETE (fixed with queue thing) Shorten the shutdown hook timeout. It is by default 30 seconds. If you stop Kafka while the app is still running,
  then when you go to shut down the app it will take 30 seconds to shut down.
* [x] DONE (fixed with queue thing)The test is flaky.
