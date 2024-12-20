# kafka-consumer-algorithms

A comparison of algorithms for consuming messages from Kafka: sequential, partially concurrent, fully concurrent.


## Overview

When designing and operating a system that uses Kafka, you will encounter different techniques for consuming messages.
The most familiar pattern is synchronously polling a batch of records, processing each one, and then committing the new
offsets back to Kafka.

While simple, this pattern suffers from bottle-necking because it's a sequential algorithm. You will eventually turn to
concurrent algorithms to increase throughput and reduce end-to-end latency in systems that need it. This project showcases
different consumer implementations and their performance characteristics. The goal of the project is that you learn
something: the poll loop, concurrent programming in Java/Kotlin, or the semantics of in-order message processing and
offset committing. Study and experiment with the code.

At a high level, this project explores Kafka consumption algorithms on two dimensions:

1. Concurrency level
2. Workload type (CPU-bound vs IO-bound)

Here is an overview of the explored algorithms, and how they perform based on the nature of the workload (CPU-bound vs IO-bound): 

| Algorithm                                               | Concurrency Level | In-Process Compute (CPU bound)                                                          | Remote Compute (IO bound)                                                                                     |
|---------------------------------------------------------|-------------------|-----------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Sequential                                              | 💻 (None)         | Slowest. It's fine when you have a CPU-bound workload and only one core.                | Slowest. It's fine when the external compute can only handle one unit of work at a time.                      |
| Concurrent across partitions within same poll           | 💻💻              | Much faster if there are multiple CPU cores, but *uneven work* is a bottleneck.         | Much faster if the remote compute can handle many requests quickly, but *uneven work* is a bottleneck.        |
| Concurrent across partitions                            | 💻💻              | ✅ Fastest general-purpose consumer. Fully saturates the workload based on CPU capacity. | ✅ Fastest general-purpose consumer. Fully saturates the workload based on the capacity of the remote compute. |
| Concurrent across partition-key groups                  | 💻💻💻            | A special case of even more concurrency if your domain permits it.                      | A special case of even more concurrency if your domain permits it.                                            |
| (What sophisticated algorithm does your domain permit?) | 💻💻💻❓           |                                                                                         |                                                                                                               |


## The Code

The test bed for these implementations is the combination of an example Kafka consumer application and a test harness.
The example app is a simple *data in, data out* Java program that consumes from a Kafka topic, transforms the data, and
then produces the transformed messages to another Kafka topic.

The example application computes prime numbers which is a CPU-intensive task. The application can also run in an
alternative mode where the computation is delegated to a fictional remote "prime computing service" and this is useful
for simulating an IO-bound workload.

Overall, this is a multi-module Gradle project with the following subprojects:

* `kafka-consumer-sequential/`
  * This the most basic Kafka consumer pattern. It processes each record in sequence (one at a time).
  * See the README in [kafka-consumer-sequential/](kafka-consumer-sequential/).
* `kafka-consumer-concurrent-across-partitions-within-same-poll/`
  * A Kafka consumer that processes messages concurrently across partitions but is sequentially confined between polls.
  * See the README in [kafka-consumer-concurrent-across-partitions-within-same-poll/](kafka-consumer-concurrent-across-partitions-within-same-poll/).
* `kafka-consumer-concurrent-across-partitions/`
  * A Kafka consumer that processes messages concurrently across partitions and decouples message processing from the poll loop.
  * See the README in [kafka-consumer-concurrent-across-partitions/](kafka-consumer-concurrent-across-partitions/).
* `kafka-consumer-concurrent-across-keys/`
  * A Kafka consumer that processes messages concurrently across partition-key groups.
  * See the README in [kafka-consumer-concurrent-across-keys/](kafka-consumer-concurrent-across-keys/).
* `kafka-consumer-concurrent-across-keys-with-coroutines/`
  * A Kafka consumer that processes messages concurrently across partition-key groups and is implemented with Kotlin coroutines.
  * See the README in [kafka-consumer-concurrent-across-keys-with-coroutines/](kafka-consumer-concurrent-across-keys-with-coroutines/).
* `runner/`
  * This is the module with a `main` method. It encapsulates the example Kafka consumer application and the test harness.


## Instructions

Follow these instructions to get up and running with Kafka, run the program, and simulate Kafka messages.

1. Pre-requisites: Java, Kafka and kcat
   * I used Java 21.
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
4. Build and run the example consumer app in standalone mode
   * ```shell
     ./gradlew runner:installDist --quiet && ./runner/build/install/runner/bin/runner standalone in-process-compute:sequential
     ```
   * Alternatively, you can run the app with one of the alternative modes. Use the following command.
   * ```shell
     ./gradlew runner:installDist --quiet && ./runner/build/install/runner/bin/runner standalone remote-compute:concurrent-across-keys-with-coroutines
     ```
   * There are other options as well. Explore the code.
5. In a new terminal, build and run a test case that exercises the app:
   * ```shell
     ./gradlew runner:installDist --quiet && ./runner/build/install/runner/bin/runner test-one-message
     ```
   * Try the other test scenarios.
   * ```shell
     ./gradlew runner:installDist --quiet && ./runner/build/install/runner/bin/runner test-multi-message
     ```
   * ```shell
     ./gradlew runner:installDist --quiet && ./runner/build/install/runner/bin/runner load-batch
     ```
6. Stop Kafka with:
   * ```shell
     ./scripts/stop-kafka.sh
     ```
7. Stop the app
   * Send `Ctrl+C` to the terminal where it's running


## Performance

You should be skeptical of performance results, because they are so often a combination of misleading, biased, and most
of all just plain wrong. It's difficult to operate a clean room environment because there is often a huge surface area
of configuration in the software being tested and of course you have little control over the OS or virtualized OS that's
running the program, the firmware of the storage, etc. That said, here are the performance results of running the
"load-all" scenario:


### Load (see the code for the details)

| Consumer Algorithm                            | Type | Final Throughput (msg/s) | Total Time (s) |
|-----------------------------------------------|------|--------------------------|----------------|
| sequential                                    | CPU  | 0.76                     | 13.14          |
| concurrent-across-partitions-within-same-poll | CPU  | 1.50                     | 6.67           |
| concurrent-across-partitions                  | CPU  | 1.40                     | 7.16           |
| concurrent-across-keys                        | CPU  | 1.70                     | 5.88           |
| concurrent-across-keys-with-coroutines        | CPU  | 1.70                     | 5.87           |
| sequential                                    | I/O  | 0.99                     | 10.06          |
| concurrent-across-partitions-within-same-poll | I/O  | 1.98                     | 5.04           |
| concurrent-across-partitions                  | I/O  | 1.81                     | 5.53           |
| concurrent-across-keys                        | I/O  | 2.21                     | 4.52           |
| concurrent-across-keys-with-coroutines        | I/O  | 2.21                     | 4.53           |


### Uneven Load (see the code for the details)

| Consumer Algorithm                            | Type | Final Throughput (msg/s) | Total Time (s) |
|-----------------------------------------------|------|--------------------------|----------------|
| sequential                                    | CPU  | 0.77                     | 25.93          |
| concurrent-across-partitions-within-same-poll | CPU  | 0.78                     | 25.67          |
| concurrent-across-partitions                  | CPU  | 1.21                     | 16.59          |
| concurrent-across-keys                        | CPU  | 1.84                     | 10.87          |
| concurrent-across-keys-with-coroutines        | CPU  | 1.84                     | 10.90          |
| sequential                                    | I/O  | 0.99                     | 20.12          |
| concurrent-across-partitions-within-same-poll | I/O  | 0.99                     | 20.13          |
| concurrent-across-partitions                  | I/O  | 1.53                     | 13.07          |
| concurrent-across-keys                        | I/O  | 2.21                     | 9.06           |
| concurrent-across-keys-with-coroutines        | I/O  | 2.21                     | 9.06           |

Mostly what I expected, but I'm confused how `concurrent-across-partitions-within-same-poll` is faster than
`concurrent-across-partitions`. Even in a small sample, the difference is significant enough.


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [ ] Consider a "RecordProcessorWithContext" interface and high-level consumer. This can give context of previously
  processed messages and upcoming ones. You should be able to express features like "debounce". Messages for the same
  key would be fused/bundled together.
* [ ] Why is the consumer group so slow to start up and become registered. It's like 5 seconds (at least for the
  coroutines consumer).
* [x] DONE Table of perf results. 'compute mode + test flavor' on the Y axis, 'consumer type' on the X axis. The values are
  throughput and latency. Actually maybe a throughput table separate from the latency table. Consider other options
  too.
   * DONE (This is by design I just keep forgetting all the dimensions. In "uneven", the ten records in the first poll
     are all for the same partition so we get no concurrency. Same with the second poll.) Follow up on the numbers.
* [x] DONE Automate the tests.
* [ ] Defect. Test harness doesn't quit on exception (e.g. timeout waiting for records)
* [x] DONE I don't need "topic" field in any of the consumers?
* [x] DONE Consider removing the app module because it's all just a test anyway. I need this so I can automate running a
  whole test suite which is too much to do manually at this point. This module has morphed from the original "kafka-in-kafka-out"
  vision to comparing algorithms. I think that's good.
* [ ] Review the start/stop logic. This is always so hard to get right.
* [x] DONE Steady or staccato load. I want to see the 10-20 messages produced in individual moments. Also, consider
  renaming the basic load and uneven loads to "batchy" and "batch-uneven".
* [ ] Use "pause" to fine-grain control the in-flight work instead of stopping the whole intake. We basically need to
  have an uninterrupted rhythm of polls. 
* [ ] Look into the metrics middleware again. I want to inject that from the runner and then continually print out the
  state of the consumer. I know we have out-of-process metrics on the test runner side, but I want to see what the
  in-flight work is and if a partition is paused.
* [ ] Reconsider the limitations of `concurrent-across-partitions-within-same-poll`. Does a given `poll` call only get
  messages for a subset of partitions under certain conditions like the broker doesn't have all the partitions, or
  something about primary/secondary? Because if a `poll` only gets messages for one partition, there is no concurrency
  benefit.
* [ ] Reduce the max poll and reduce the in-flight limit. As with everything, reducing down to the smallest reproducible
  configuration helps us focus on concepts. Shedding complexity begets shedding more complexity.


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
* [x] DONE Reconsider "uneven" load test. Do I need yet another consumer which is async but only on partition? I think so. I
  need a way to make a case for the key-based processing.
    * DONE Create a "record-at-a-time" consumer. (now renamed "sequential")
    * DONE Create a "parallel-within-same-poll"
    * DONE Create a basic async consumer. Thread pool? I'll allow the existing virtual thread consumer to showcase virtual threads.
      I think it's good to jump to async on partition before escalating to async on partition-key.
    * DONE Is the existing "batchy" scenario good enough? We're trying to show that "parallel in same poll" and "async" are both
      good helping throughput in general, but async is better for uneven loads because the parallel-poll one will suffer
      from bottle-necking on the slowest partition. `load-batchy` for parallel yields an elapsed time of 26s and for async
      an elapsed time of 16s.
    * DONE Rename batchy to uneven (second time I've changed the name)
* [x] DONE Reflow the docs to highlight `concurrent-across-partitions` as the most interesting one. The key-based stuff is cool, but it is
  not the core insight: async processing is. Key-based processing is just another evolution of that, not a phase change.
* [x] DONE reflow main docs
    * DONE Two dimension view: concurrency and workload (CPU vs IO).
    * DONE concurrent language instead of async (I'm waffling on this, but I like highlighting the algorithm language
      (sequential/parallel) instead of the programming idiom language (blocking/async)).
    * DONE Turn "parallel within poll" to just concurrent within poll. "Stream.parallel" is a mirage anyway, it's just
      multithreaded and its up to the OS/hardware to actually give us parallelism.
* [x] DONE (or wait... just use virtual threads? Concurrent programming and APIs are so hard) For the "in-process-compute" mode, configure a thread pool only of the core count. I really want to contrast the
  constraint difference of CPU-bound and IO-bound workloads. A CPU-bound workload can't be parallelized beyond the core
  count. Mechanical sympathy.


## Reference

* [Kafka producer configs](https://kafka.apache.org/documentation/#producerconfigs)
* [Wikipedia: *Concurrent computing*](https://en.wikipedia.org/wiki/Concurrent_computing)
