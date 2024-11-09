# test-harness

This is a test harness for executing JUnit-based tests against the *Kafka in, Kafka out* program (see `../app`).

These tests are executed *out-of-process* from the `app` process. Perhaps the most important point of the `kafka-consumer-algorithms`
project is to showcase the independence of `app` and how it has a clear and well-defined interface: Kafka topics. The
interface is as simple as sending messages to Kafka topic `input` and reading messages from Kafka topic `output`.
In fact, we could write the `test-harness` using a completely different technology stack, like GoLang or C, because the interface
between the `test-harness` and `app` is just Kafka. There are [Kafka clients in a long list of programming languages](https://cwiki.apache.org/confluence/display/KAFKA/Clients).
