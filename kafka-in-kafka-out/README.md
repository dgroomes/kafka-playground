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
     ./gradlew app:installDist && ./app/build/install/app/bin/app
     ```
5. In a new terminal, build and run a test case that exercises the app:
   * ```shell
     ./gradlew test-harness:installDist && ./test-harness/build/install/test-harness/bin/test-harness
     ```
6. Stop Kafka with:
   * ```shell
     ./scripts/stop-kafka.sh
     ```
7. Stop the `app` program
   * Send `Ctrl+C` to the terminal where it's running


## Simulate load

There is an additional subproject named `load-simulator/` that will simulate load against the Kafka cluster by generating
many messages and producing them to the input Kafka topic. Build and run the load simulator
program distribution with:

```shell
./gradlew load-simulator:installDist && ./load-simulator/build/install/load-simulator/bin/load-simulator 100 100 100
```


## Notes

I use Nushell and for a speedier development/experiment workflow I use the commands in the `scripts/do.nu` file. Load
them with:

```nushell
let PROJECT_DIR = (pwd)
use scripts/do.nu *
```

With these commands, I can start/stop Kafka, create the topics, watch the consumer groups, etc. Note that the
`let PROJECT_DIR` trick is necessary because Nushell doesn't support the `$env.FILE_PWD` in modules (see <https://github.com/nushell/nushell/issues/9776>).


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [x] DONE (Yeah the app just takes some time. So increasing the timeout on the test side works. I wonder if there is a
  config to let if start up faster though (less wait?)). The tests appear flaky, but it only happens when I start the app and then quickly run the tests. I think there's
  some sleep in the Kafka consumer at startup time that's the problem. I would love to be able to key off of some "ready"
  event or something.
* [x] DONE Consider making the test harness just a `public static void main`. That way, can I use the main thread as the
  consumer thread (and remove all the test dependencies)?
* [ ] Consider making just one module aside from the 'app' module. Maybe just a 'controller', 'admin', or something? In
  it, it can do the observability stuff, the test, the load simulation, etc. 
* [x] DONE Consider making the logic a slow function, like a sort, as a useful way to contrast a multicore
  configuration vs single core. I don't want to just use sleeps because they don't stress the CPU.
* [x] DONE Delete the compression stuff. That might fit better in a "kafka administration" module. I still think it's
  interesting, but I want this module focused on the design of the app.
* [ ] Parallel processing.


## Finished Wish List Items

These items were either completed or skipped.

* [x] DONE Simulate processing slowness in `app/`. This will have the effect of the consumer group timing out with the Kafka
  broker and being removed from the group. This is a classic problem.
* [x] DONE (Fixed!) The test is flaky. The first time it runs, it fails (at least in my own test runs) but subsequent runs it succeeds. I
  want to dive deeper into what the consumer is doing. When is it ready?
* [x] DONE (I don't know, sometimes the tests are still flaky, and I'm not sure why) Upgrade to Java 17. For some reason, the test harness fails when executing with Java 17.


## Reference

* [Kafka producer configs](https://kafka.apache.org/documentation/#producerconfigs)
