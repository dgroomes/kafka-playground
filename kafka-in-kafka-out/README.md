# kafka-in-kafka-out

A simple *Kafka in, Kafka out* Java program accompanied by an out-of-process test harness.

Let's make a simple program that reads data from a Kafka topic and outputs data to another Kafka topic in a way that models
a so-called [*pure function*](https://en.wikipedia.org/wiki/Pure_function). A pure function takes data in and puts data
out. This style of program is a perfect match for Kafka. 

## Architecture

This is a multi-module Gradle project with the following sub-projects:

* `app/`
  * This is the *Kafka in, Kafka out* Java program
  * See [app/README.md](app/README.md).
* `test-harness/`
  * This is a [test harness](https://en.wikipedia.org/wiki/Test_harness) for running and executing automated tests against `app`.
  * See [test-harness/README.md](test-harness/README.md).
* `load-simulator/`
  * Simulate load by generate many Kafka messages

### Instructions

* Use Java 11
* Install Kafka and `kafkacat`:
  * `brew install kafka`
  * `brew install kafkacat`
* Running the application and the `test-harness` depend on a locally running Kafka instance. Start Kafka with:
  * `./scripts/start-kafka.sh`
* In a new terminal, build and run the `app` program with:
  * `./gradlew app:run`
* In a new terminal, build and run the tests with:
  * `./gradlew test-harness:test`
* Stop Kafka with:
  * `./scripts/stop-kafka.sh`

### Simulate load

There is an additional sub-project named `load-simulator/` that will simulate load against the Kafka cluster by generating
many messages and producing them to the same Kafka topic that `app/` consumes: "input-text". Build and run the load
simulator with:

```
./gradlew load-simulator:run --args "10_000_000"

# Optionally, enable compression
PRODUCER_COMPRESSION=lz4 ./gradlew load-simulator:run --args "10_000_000"
```

The integer argument is the number of messages that it will generate. After it's done, you can see the volume of data that
was actually persisted in the the Kafka broker's data directory:

```
du -h /usr/local/var/lib/kafka-logs/input-text-0/ \
      /usr/local/var/lib/kafka-logs/quoted-text-0/
```

You should notice much lower volumes of data for the "input-text" Kafka topic when using compression versus without compression.
Furthermore, I would like to experiment with different settings on the consumer side too.

Also, to get much more throughput on the `app/`, you can configure it to commit offsets and produce asynchronously by setting
an environment variable before running it:

```
SYNCHRONOUS=false ./gradlew app:run
```

### Wish list

Items I wish to implement for this project:

* DONE (Fixed!) The test is flaky. The first time it runs, it fails (at least in my own test runs) but subsequent runs it succeeds. I
  want to dive deeper into what the consumer is doing. When is it ready? 

### Referenced materials

* [Kafka producer configs](https://kafka.apache.org/documentation/#producerconfigs)
