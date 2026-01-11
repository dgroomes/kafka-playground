# kafka-playground

📚 Learning and experimenting with Apache Kafka <https://kafka.apache.org/>.


## Standalone subprojects

This repository illustrates different concepts, patterns and examples via standalone subprojects. Each subproject is
completely independent of the others and do not depend on the root project. This _standalone subproject constraint_
forces the subprojects to be complete and maximizes the reader's chances of successfully running, understanding, and
re-using the code.

The subprojects include:


### `interactive/`

An interactive program to consume from Kafka using the _plain-ole'/regular/vanilla_ Java [KafkaConsumer](https://github.com/apache/kafka/tree/40b0033eedf823d3bd3c6781cfd871a949c5827e/clients/src/main/java/org/apache/kafka/clients/consumer).

**TIP**: This is a good project to start with you if you are just learning about Kafka, or more specifically you are
learning how to interface with Kafka via a Java program.

See the README in [interactive/](interactive/). 


### `kafka-consumer-algorithms/`

A comparison of algorithms for consuming messages from Kafka: sequential, partial parallelism, and asynchronous.

See the README in [kafka-consumer-algorithms/](kafka-consumer-algorithms/).


### `connection-check/`

Use the Java Kafka client to check for a connection to a Kafka cluster. Sometimes, this is called a *health check*.

See the README in [connection-check/](connection-check/).


### `streams/`

A basic [Kafka Streams](https://kafka.apache.org/documentation/streams/) application.

See the README in [streams/](streams/).


### `streams-zip-codes/`

An intermediate Kafka Streams project that aggregates ZIP code data.

See the README in [streams-zip-codes/](streams-zip-codes/).


### `spring-seekable/`

A basic [Spring Kafka](https://spring.io/projects/spring-kafka) application with a "seekable" Kafka listener.

See the README in [spring-seekable/](spring-seekable/).


### `spring-headers/`

A basic [Spring Kafka](https://spring.io/projects/spring-kafka) application that showcases the Spring framework behavior
around Kafka message headers.

See the README in [spring-headers/](spring-headers/).


### `spring-errors/`

A basic [Spring Kafka](https://spring.io/projects/spring-kafka) application that showcases the Spring framework features
and behavior around Kafka error handling.

See the README in [spring-errors/](spring-errors/).


### `spring-barebones/`

A simple Java program to process messages from a Kafka topic using abstractions from [Spring for Apache Kafka](https://spring.io/projects/spring-kafka).

See the README in [spring-barebones/](spring-barebones/).


### `spring-multi-broker`

A Spring Kafka application that consumes from multiple Kafka brokers.

See the README in [spring-multi-broker/](spring-multi-broker/).


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [ ] Consider a subproject that shows metrics. Kafka I think already has JMX metrics or something, but I'm not totally
  up to speed on that.
