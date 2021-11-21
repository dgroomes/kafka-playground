# interactive

Let's create an interactive program to consume from Kafka using the vanilla KafkaConsumer (no frameworks) and learn 
something in the process!

---

Uses the official Apache Kafka Java client <https://github.com/apache/kafka/tree/40b0033eedf823d3bd3c6781cfd871a949c5827e/clients/src/main/java/org/apache/kafka/clients/consumer>.

This project is deliberately implemented in a *vanilla* way (no frameworks, no frills, no alternative toolchains) so
that it can let the components of Kafka shine. The project should help you actually learn something about Kafka and the
Kafka client.

### Instructions

* Use Java 11
* Install Kafka and `kcat`:
  * `brew install kafka`
  * `brew install kcat`
* Running the application and the test cases depend on a locally running Kafka instance. Use the `startKafka` and 
  `stopKafka` commands (see [`commands.sh`](#commandssh)) to run Kafka.
* In a new terminal, build and run the program with `build && run`
* In a new terminal, produce some test data with `produce`. You should see the application react with new logs. Next,
  try `produce 10`.
* In the application terminal, start experimenting by typing in any of the commands: "start", "stop", "reset", "rewind",
  "current-offsets". Continue to experiment!

### `commands.sh`

Source the `commands.sh` file using `source commands.sh` which will load your shell with useful 
commands. Commands include:

  * `startKafka` start Kafka
  * `stopKafka` stop Kafka
  * `createTopic` create the Kafka topic
  * `build` build (without running the tests)
  * `runTests` run the tests
  * `run` run the app
  * `consume` consume from the `my-messages` Kafka topic
  * `produce` produce a test message to the `my-messages` Kafka topic 
  * `currentOffsets` get current Kafka topic offsets for the `my-group` group 

### Wish list

General clean ups, TODOs and things I wish to implement for this project:

  * DONE Implement a command to list Kafka client side metrics  

### Notes

A neat trick to check for processes that are using a port is to use the `lsof` command. For example, use

```echo "kafka port?" && lsof -i :9092```

to check if Kafka is running. 

### Referenced materials

* [Official Java docs: *Monitoring and Management Using JMX Technology*](https://docs.oracle.com/en/java/javase/11/management/monitoring-and-management-using-jmx-technology.html)
* [Kafka consumer config](https://kafka.apache.org/documentation.html#consumerconfigs)
