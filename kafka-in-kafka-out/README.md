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
  * Simulate load by generate many Kafka messages
* `kafka-high-level-consumer/`
  * Various scheduling and acknowledgement algorithms for consuming Kafka messages.
  * See the README in [kafka-high-level-consumer/](kafka-high-level-consumer/).


## Instructions

Follow these instructions to get up and running with Kafka, run the program, and simulate Kafka messages.

1. Pre-requisites: Java, Kafka and kcat
   * I used Java 21 installed via SDKMAN.
   * I used Kafka 3.8.0 installed via Homebrew.
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
4. Build and run the `app` program distribution
   * ```shell
     ./gradlew app:installDist --quiet && ./app/build/install/app/bin/app sync
     ```
   * Alternatively, you can run the `app` program with one of the asynchronous consumers. Use the following command.
   * ```shell
     ./app/build/install/app/bin/app async-coroutines
     ```
5. In a new terminal, build and run a test case that exercises the app:
   * ```shell
     ./gradlew test-harness:installDist --quiet && ./test-harness/build/install/test-harness/bin/test-harness one-message
     ```
   * Try the other test scenarios.
   * ```shell
     ./test-harness/build/install/test-harness/bin/test-harness multi-message
     ```
   * ```shell
     ./test-harness/build/install/test-harness/bin/test-harness load cpu-intensive
     ```
6. Stop Kafka with:
   * ```shell
     ./scripts/stop-kafka.sh
     ```
7. Stop the `app` program
   * Send `Ctrl+C` to the terminal where it's running


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [ ] Approximate a slow external collaborator? For realism, we want to approximate both slow CPU intensive work and
  slow IO.
* [ ] Consider a "RecordProcessorWithContext" interface and high-level consumer. This can give context of previously
  processed messages and upcoming ones. You should be able to express features like "debounce". Messages for the same
  key would be fused/bundled together.
* [ ] Why is the consumer group so slow to start up and become registered. It's like 5 seconds (at least for the
  coroutines consumer).


## Finished Wish List Items

These items were either completed or skipped.

* [x] DONE Simulate processing slowness in `app/`. This will have the effect of the consumer group timing out with the Kafka
  broker and being removed from the group. This is a classic problem.
* [x] DONE (Fixed!) The test is flaky. The first time it runs, it fails (at least in my own test runs) but subsequent runs it succeeds. I
  want to dive deeper into what the consumer is doing. When is it ready?
* [x] DONE (I don't know, sometimes the tests are still flaky, and I'm not sure why) Upgrade to Java 17. For some reason, the test harness fails when executing with Java 17.
* [x] DONE (Yeah the app just takes some time. So increasing the timeout on the test side works. I wonder if there is a
  config to let if start up faster though (less wait?)). The tests appear flaky, but it only happens when I start the app and then quickly run the tests. I think there's
  some sleep in the Kafka consumer at startup time that's the problem. I would love to be able to key off of some "ready"
  event or something.
* [x] DONE Consider making the test harness just a `public static void main`. That way, can I use the main thread as the
  consumer thread (and remove all the test dependencies)?
* [x] DONE Consider making just one module aside from the 'app' module. Maybe just a 'controller', 'admin', or something? In
  it, it can do the observability stuff, the test, the load simulation, etc.
* [x] DONE Consider making the logic a slow function, like a sort, as a useful way to contrast a multicore
  configuration vs single core. I don't want to just use sleeps because they don't stress the CPU.
* [x] DONE Delete the compression stuff. That might fit better in a "kafka administration" module. I still think it's
  interesting, but I want this module focused on the design of the app.
* [x] DONE (as per usual, sophistication often reduces performance) Async and parallelism processing.
* [x] DONE Less error handling. Error handling is critical, but I'm already trying to showcase plenty of scheduling and
  coordinating concerns with regard to processing message and committing offsets. Leave out error handling but be clear
  about it.
* [x] DONE Consider using executor and tasks to de-couple polling from committing in the virtual thread implementation. To
  be symmetric with the coroutine implementation.
* [x] DONE (partial; there's [no support for virtual threads](https://github.com/oracle/visualvm/issues/462)) VisualVM
* [x] DONE Kotlin coroutine based "key/async" high-level consumer. I want to compare and contrast the
  programming model. My guess and hope is that I can use ["thread confinement"](https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html#thread-confinement-fine-grained)
  when using coroutines to get the semantics I need but without using so many constructs in my own code (dictionaries,
  queues, futures, etc.)
    * DONE Get the poll loop working
    * DONE offset committing
* [x] DONE More validation. Do tests beyond just one message. We need multiple messages for a key, and multiple partitions.
* [x] DONE Limit intake in the coroutine consumer. Do this in the same way as the virtual thread consumer with
  the dual "queue/processed" counters.
* [x] DONE (Seems to work, but hard to know with concurrent programming) Defect. The virtual thread consumer is blocked on the poll loop. I didn't schedule the work correctly. I think I
  want two different virtual thread executors, so that each one as its own platform thread? Is that possible? UPDATE: No,
  all virtual threads management is done out of user control.
* [x] DONE Consistent and fleshed out reporting logging. I want apples-to-apples between the sync/coroutine/virtual-thread
  consumers. While it may be more engineered to export metrics and do the reporting and visualization in an outside tool,
  the buck has to stop somewhere. Let's keep it legible.
* [x] DONE (Prime finding) Use a pure CPU-intensive function. Sorting is boosted so strongly the memory speed that it's actually 10 time slower
  to parallelize it (I still barely understand that... maybe if I did huge lists that would amortize away). Regardless,
  the affect is pronounced and makes for a bad demo. Can we do prime factorization or fibonacci or something?
* [x] DONE (duh.. needed to flush) Defect. When producing small amounts of messages (somewhere less than 100), the messages just don't
  appear... Defect in my producer.


## Reference

* [Kafka producer configs](https://kafka.apache.org/documentation/#producerconfigs)
