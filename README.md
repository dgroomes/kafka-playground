# kafka-playground

📚 Learning and experimenting with Apache Kafka <https://kafka.apache.org/>.

---

## Standalone sub-projects

This repository illustrates different concepts, patterns and examples via standalone sub-projects. Each sub-project is
completely independent of the others and do not depend on the root project. This _standalone sub-project constraint_
forces the sub-projects to be complete and maximizes the reader's chances of successfully running, understanding, and
re-using the code.

The sub-projects include:

### `interactive/`

An interactive program to consume from Kafka using the _plain-ole'/regular/vanilla_ Java [KafkaConsumer](https://github.com/apache/kafka/tree/40b0033eedf823d3bd3c6781cfd871a949c5827e/clients/src/main/java/org/apache/kafka/clients/consumer).

**TIP**: This is a good project to start with you if you are just learning about Kafka, or more specifically you are
learning how to interface with Kafka via a Java program.

See the README in [interactive/](interactive/). 

### `kafka-in-kafka-out/`

A simple *Kafka in, Kafka out* Java program accompanied by an out-of-process test harness.

See the README in [kafka-in-kafka-out/](kafka-in-kafka-out/).

### `streams/`

A basic [Kafka Streams](https://kafka.apache.org/documentation/streams/) application.

See the README in [streams/](streams/).

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

### `spring-interfaces/`

NOT YET IMPLEMENTED

A basic [Spring Kafka](https://spring.io/projects/spring-kafka) application that demonstrates how to use Spring Kafka
by implementing Spring Kafka interfaces instead of using annotations like `KafkaListener`.

See the README in [spring-interfaces/](spring-interfaces/).

### `utility-scripts/`

Utility Bash scripts for starting and stopping Kafka and Zookeeper.

See the README in [utility-scripts/](utility-scripts/).
