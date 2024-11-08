# kafka-consumer-abstractions

Various code abstractions and scheduling algorithms for consuming from Kafka.


## Overview

When designing and operating a system that uses Kafka, you will encounter different techniques for consuming messages.
The most familiar pattern is synchronously polling a batch of records, processing them all, and then committing the new
offsets back to Kafka.

While simple, this pattern suffers from bottle-necking. You will eventually turn to other patterns to increase
throughput in systems that need it. This project showcases different consumer implementations and the goal of the
project is that you learn something. Please study and experiment with the code.

The test bed for these abstractions is the combination of an example Kafka consumer application and an out-of-process
test harness. The example app is a simple *data in, data out* Java program that consumes from a Kafka topic, transforms
the data, and then produces the transformed messages to another Kafka topic.

This is a multi-module Gradle project with the following subprojects:

* `kafka-consumer-sequential/`
  * This the most basic Kafka consumer pattern. It processes each record in sequence (one at a time).
  * See the README in [kafka-consumer-sequential/](kafka-consumer-sequential/).
* `kafka-consumer-parallel-within-same-poll/`
  * A sequential consumer with some parallelization. For records returned by a poll, records are processed with parallelism equal to the number of partitions.  
  * See the README in [kafka-consumer-parallel-within-same-poll/](kafka-consumer-parallel-within-same-poll/).
* `kafka-consumer-with-coroutines/`
  * An asynchronous Kafka consumer and message processor implemented with coroutines.
  * See the README in [kafka-consumer-with-coroutines/](kafka-consumer-with-coroutines/).
* `kafka-consumer-with-virtual-threads/`
  * An asynchronous Kafka consumer and message processor implemented with virtual threads.
  * See the README in [kafka-consumer-with-virtual-threads/](kafka-consumer-with-virtual-threads/).
* `example-consumer-app/`
  * This is the *Kafka in, Kafka out* Java program. Its domain is computing prime numbers; a very CPU-intensive task. 
  * See the README in [example-consumer-app/](example-consumer-app/).
* `test-harness/`
  * This is a [test harness](https://en.wikipedia.org/wiki/Test_harness) for running and executing automated tests against `example-consumer-app`.
  * Simulates load by generating many Kafka messages
  * See the README in [test-harness/](test-harness/).

The example application computes prime numbers which is a CPU-intensive task. The application can also run in an
alternative mode where the computation is delegated to a fictional remote "prime computing service". Let's review the
modes of operation:

|                            | **In-Process Compute (CPU bound)**                         | **Remote Compute (IO bound)**                                                                                                                             |
|----------------------------|------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Sequential**             | TODO                                                       | TODO                                                                                                                                                      |
| **Parallel within poll**   | Can be spiky if the units of work are unevenly distributed | ❌ This is problematic. The consumer concurrency is limited by the number of CPU cores, but ideally we should only be bottle-necked by the remote service. |
| **Async: coroutines**      | ✅ Smooth and saturates the CPU                             | ✅ Smooth and saturates the remote service                                                                                                                 |
| **Async: virtual threads** | ✅ Smooth and saturates the CPU                             | ✅ Smooth and saturates the remote service                                                                                                                 |


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
4. Build and run the `example-consumer` program distribution
   * ```shell
     ./gradlew example-consumer-app:installDist --quiet && ./example-consumer-app/build/install/example-consumer-app/bin/example-consumer-app in-process-compute:parallel-within-same-poll-consumer
     ```
   * Alternatively, you can run the `example-consumer-app` program with one of the alternative consumers. Use the following command.
   * ```shell
     ./gradlew example-consumer-app:installDist --quiet && ./example-consumer-app/build/install/example-consumer-app/bin/example-consumer-app in-process-compute:coroutines-consumer
     ```
   * There are other options as well. Explore the code.
5. In a new terminal, build and run a test case that exercises the app:
   * ```shell
     ./gradlew test-harness:installDist --quiet && ./test-harness/build/install/test-harness/bin/test-harness one-message
     ```
   * Try the other test scenarios.
   * ```shell
     ./test-harness/build/install/test-harness/bin/test-harness multi-message
     ```
   * ```shell
     ./test-harness/build/install/test-harness/bin/test-harness load
     ```
6. Stop Kafka with:
   * ```shell
     ./scripts/stop-kafka.sh
     ```
7. Stop the `example-consumer-app`
   * Send `Ctrl+C` to the terminal where it's running


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [ ] Consider a "RecordProcessorWithContext" interface and high-level consumer. This can give context of previously
  processed messages and upcoming ones. You should be able to express features like "debounce". Messages for the same
  key would be fused/bundled together.
* [ ] Why is the consumer group so slow to start up and become registered. It's like 5 seconds (at least for the
  coroutines consumer).
* [ ] IN PROGRESS Reconsider "uneven" load test. Do I need yet another consumer which is async but only on partition? I think so. I
  need a way to make a case for the key-based processing. 
   * DONE Create a "record-at-a-time" consumer.
   * Create a "parallel" 
* [ ] Table of perf results. 'compute mode + test flavor' on the Y axis, 'consumer type' on the X axis. The values are
  throughput and latency. Actually maybe a throughput table separate from the latency table. Consider other options
  too.


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
* [x] DONE Approximate a slow external collaborator? For realism, we want to approximate both slow CPU intensive work and
  slow IO.
* [x] DONE Change `load cpu-intensive` language to just `small medium large` or something because now I've decided that the
  app encapsulates the compute option.
* [x] DONE Do message processing count only in the test harness. This will shave some code nicely across the consumers.
* [x] DONE Track full message wait time: from when it's produced to when it's output message is created. It's not enough to
  calculate processing time because, for example, "all messages are blocked until the end" is not as good as "only some
  messages experience the whole time and many are completed earlier".
* [x] DONE Turn uneven into "batchy".
* [x] DONE (annoyingly complicated and verbose) Log when the consumer is assigned. It's annoying to have to guess and over wait until I kick off a
  load test. Maybe it's enough to just seek to the end in a blocking way?


## Reference

* [Kafka producer configs](https://kafka.apache.org/documentation/#producerconfigs)
