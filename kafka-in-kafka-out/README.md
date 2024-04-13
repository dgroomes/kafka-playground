# kafka-in-kafka-out

A simple *Kafka in, Kafka out* Java program accompanied by an out-of-process test harness.


## Overview

Let's make a simple program that reads data from a Kafka topic and outputs data to another Kafka topic in a way that models
a so-called [*pure function*](https://en.wikipedia.org/wiki/Pure_function). A pure function takes data in and puts data
out. This style of program is a perfect match for Kafka. 

This is a multi-module Gradle project with the following subprojects:

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

1. Pre-requisites: Java, Kafka and kcat
   * I used Java 21 installed via SDKMAN.
   * I used Kafka 3.7.0 installed via Homebrew.
   * I used kcat 1.7.0 installed via Homebrew.
   * Tip: check your HomeBrew-installed package versions with a command like the following.
   * ```shell
     brew list --versions kafka
     ```
2. Start Kafka:
   * ```shell
     ./scripts/start-kafka.sh
     ```
3. Create the Kafka topics:
   * ```shell
     ./scripts/create-topics.sh
     ```
4. In a new terminal, build the `app` program distribution
   * ```shell
     ./gradlew app:installDist
     ```
5. Run the `app` program
   * ```shell
     ./app/build/install/app/bin/app
     ```
6. In a new terminal, build and run the tests with:
   * ```shell
     ./gradlew test-harness:test
     ```
7. Stop Kafka with:
   * ```shell
     ./scripts/stop-kafka.sh
     ```
8. Stop the `app` program
   * Send `Ctrl+C` to the terminal where it's running


## Simulate load

There is an additional subproject named `load-simulator/` that will simulate load against the Kafka cluster by generating
many messages and producing them to the same Kafka topic that `app/` consumes: "input-text". Build load simulator
program distribution with:

```shell
./gradlew load-simulator:installDist
```

Run the load simulator with:

```shell
./load-simulator/build/install/load-simulator/bin/load-simulator 10_000_000
```

Optionally, try it with compression enabled:

```shell
PRODUCER_COMPRESSION=lz4 ./load-simulator/build/install/load-simulator/bin/load-simulator 10_000_000
```

The integer argument is the number of messages that it will generate. After it's done, you can see the volume of data that
was actually persisted in the Kafka broker's data directory:

```shell
du -h scripts/tmp-kafka-data-logs/input-text-0 \
      scripts/tmp-kafka-data-logs/quoted-text-0
```

You should notice much lower volumes of data for the "input-text" Kafka topic when using compression versus without compression.
Furthermore, I would like to experiment with different settings on the consumer side too.

Also, to get much more throughput on the `app/`, you can configure it to commit offsets and produce asynchronously by setting
an environment variable before running it:

```shell
SYNCHRONOUS=false ./app/build/install/app/bin/app
```

Also, simulate slow processing time for the "quote" procedure by setting an environment variable before running the
program. Each message will take this amount of additional time, in milliseconds, to be processed by the "quote"
procedure:

```shell
SIMULATED_PROCESSING_TIME=1_000 ./app/build/install/app/bin/app
```


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [ ] The tests appear flaky, but it only happens when I start the app and then quickly run the tests. I think there's
  some sleep in the Kafka consumer at startup time that's the problem. I would love to be able to key off of some "ready"
  event or something.


## Finished Wish List Items

These items were either completed or skipped.

* [x] DONE Simulate processing slowness in `app/`. This will have the effect of the consumer group timing out with the Kafka
  broker and being removed from the group. This is a classic problem.
* [x] DONE (Fixed!) The test is flaky. The first time it runs, it fails (at least in my own test runs) but subsequent runs it succeeds. I
  want to dive deeper into what the consumer is doing. When is it ready?
* [x] DONE (I don't know, sometimes the tests are still flaky and I'm not sure why) Upgrade to Java 17. For some reason, the test harness fails when executing with Java 17.


## Reference

* [Kafka producer configs](https://kafka.apache.org/documentation/#producerconfigs)
