# interactive

Let's create an interactive program to consume from Kafka using the vanilla KafkaConsumer (no frameworks) and learn 
something in the process!


## Overview

Uses the official Apache Kafka Java client <https://github.com/apache/kafka/tree/40b0033eedf823d3bd3c6781cfd871a949c5827e/clients/src/main/java/org/apache/kafka/clients/consumer>.

This project is deliberately implemented in a *vanilla* way (no frameworks, no frills, no alternative toolchains) so
that it can let the components of Kafka shine. The project should help you actually learn something about Kafka and the
Kafka client.


## Instructions

Follow these instructions to get up and running with Kafka, run the sample program, and experiment with Kafka messages.

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
   * Running the application depends on a locally running Kafka instance. Use the `startKafka` and 
     `stopKafka` commands (see [`commands.sh`](#commandssh)) to run Kafka. Use the following commands to source the
     commands file and then start Kafka.
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
5. Produce some test messages
   * Open a new terminal and source the commands file. Then execute the following command.
   * ```shell
     produce
     ```
   * You should see the application react with new logs!
   * Next, produce multiple messages with the following command.
   * ```shell
     produce 10
     ```
6. Experiment!
   * In the terminal you used to start the program, experiment by typing in any of the commands: "stop", "start",
     "reset", "rewind", "current-offsets". Continue to experiment!
7. When done, stop Kafka
   * ```shell
     stopKafka
     ```


## `commands.sh`

Source the `commands.sh` file using `source commands.sh` which will load your shell with useful 
commands. Commands include:

  * `startKafka` start Kafka
  * `stopKafka` stop Kafka
  * `createTopic` create the Kafka topic
  * `build` build
  * `run` run the app
  * `consume` consume from the `my-messages` Kafka topic
  * `produce` produce a test message to the `my-messages` Kafka topic 
  * `currentOffsets` get current Kafka topic offsets for the `my-group` group 


## Wish list

General clean ups, TODOs and things I wish to implement for this project:

  * [x] DONE Implement a command to list Kafka client side metrics  


## Notes

A neat trick to check for processes that are using a port is to use the `lsof` command. For example, use

```shell
echo "kafka port?" && lsof -i :9092
```

to check if Kafka is running. 


## Reference

* [Official Java docs: *Monitoring and Management Using JMX Technology*](https://docs.oracle.com/en/java/javase/11/management/monitoring-and-management-using-jmx-technology.html)
* [Kafka consumer config](https://kafka.apache.org/documentation.html#consumerconfigs)
