# kafka-in-kafka-out

A simple *Kafka in, Kafka out* Java program accompanied by an out-of-process test harness.

## Overview

Let's make a simple program that reads data from a Kafka topic and outputs data to another Kafka topic in a way that models
a so-called [*pure function*](https://en.wikipedia.org/wiki/Pure_function). A pure function takes data in and puts data
out. This style of program is a perfect match for Kafka. 

This is a multi-module Gradle project with the following sub-projects:

* `app/`
  * This is the *Kafka in, Kafka out* Java program
  * See the README in [app/](app/).
* `test-harness/`
  * This is a [test harness](https://en.wikipedia.org/wiki/Test_harness) for running and executing automated tests against `app`.
  * See the README in [test-harness/](test-harness/).
* `load-simulator/`
  * Simulate load by generate many Kafka messages


## Instructions

Follow these instructions to get up and running with Kafka, run the program, and simulate Kafka messages.

1. Use Java 17
2. Install Kafka and `kcat`:
   * ```shell
     brew install kafka
     ```
   * Note: the version I used at the time was 3.3.1_1. Check your installed version with `brew list --versions kafka`.
   * ```shell
     brew install kcat
     ```
3. Start Kafka:
   * ```shell
     ./scripts/start-kafka.sh
     ```
4. Create the Kafka topics:
   * ```shell
     ./scripts/create-topics.sh
     ```
5. In a new terminal, build and run the `app` program with:
   * ```shell
     ./gradlew app:run
     ```
6. In a new terminal, build and run the tests with:
   * ```shell
     ./gradlew test-harness:test
     ```
7. Stop Kafka with:
   * ```shell
     ./scripts/stop-kafka.sh
     ```


## Simulate load

There is an additional sub-project named `load-simulator/` that will simulate load against the Kafka cluster by generating
many messages and producing them to the same Kafka topic that `app/` consumes: "input-text". Build and run the load
simulator with:

```shell
./gradlew load-simulator:run --args "10_000_000"
```

Optionally, try it with compression enabled:

```shell
PRODUCER_COMPRESSION=lz4 ./gradlew load-simulator:run --args "10_000_000"
```

The integer argument is the number of messages that it will generate. After it's done, you can see the volume of data that
was actually persisted in the the Kafka broker's data directory:

```shell
du -h /tmp/kraft-combined-logs/input-text-0/ \
      /tmp/kraft-combined-logs/quoted-text-0/
```

You should notice much lower volumes of data for the "input-text" Kafka topic when using compression versus without compression.
Furthermore, I would like to experiment with different settings on the consumer side too.

Also, to get much more throughput on the `app/`, you can configure it to commit offsets and produce asynchronously by setting
an environment variable before running it:

```shell
SYNCHRONOUS=false ./gradlew app:run
```

Also, simulate slow processing time for the "quote" procedure by setting an environment variable before running the
program. Each message will take this amount of additional time, in milliseconds, to be processed by the "quote"
procedure:

```shell
SIMULATED_PROCESSING_TIME=1_000 ./gradlew app:run
```


## Wish List

General clean ups, TODOs and things I wish to implement for this project:

* [ ] The tests are still a bit flaky but it's rare enough that I can't reproduce it reliably. I suspect something is
  wrong with my Kafka broker config. I'd love to fix this long-standing flakiness.


### Finished Wish List Items

These items were either completed or skipped.

* [x] DONE Simulate processing slowness in `app/`. This will have the effect of the consumer group timing out with the Kafka
  broker and being removed from the group. This is a classic problem.
* [x] DONE (Fixed!) The test is flaky. The first time it runs, it fails (at least in my own test runs) but subsequent runs it succeeds. I
  want to dive deeper into what the consumer is doing. When is it ready?
* [x] DONE (I don't know, sometimes the tests are still flaky and I'm not sure why) Upgrade to Java 17. For some reason, the test harness fails when executing with Java 17.


## Reference

* [Kafka producer configs](https://kafka.apache.org/documentation/#producerconfigs)
