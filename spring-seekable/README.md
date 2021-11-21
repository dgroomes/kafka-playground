# spring-seekable

A basic Spring Kafka application with a "seekable" Kafka listener.

---

In essence, the Kafka listener base class has operations to seek to the beginning or end (not implemented) of the 
Kafka topic. This is a useful feature when executing tests or when needing to replay all Kafka 
messages from the beginning of a Kafka topic.

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
* In the application terminal, press "enter" to seek the consumer to the beginning of the topic. It should replay all 
  the messages on the topic.

### `commands.sh`

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
  
### TODO

  * DONE (wow, it was my bad all along! i never cleared the queue). Why doesn't this work? It appears to seek to the beginning but no messages get printed out that indicate any
    messages were actually re-processed.
  * OBSOLETE (fixed with queue thing) Shorten the shutdown hook timeout. It is by default 30 seconds. If you stop Kafka while the app is still running,
    then when you go to shut down the app it will take 30 seconds to shut down.
  * DONE (fixed with queue thing)The test is flaky.
