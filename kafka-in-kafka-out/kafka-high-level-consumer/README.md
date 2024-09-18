# kafka-high-level-consumer

Various scheduling and acknowledgement algorithms for consuming Kafka messages.


## Overview

When designing and operating a system that uses Kafka, you will encounter different techniques for consuming messages.
The most familiar pattern is synchronously polling a batch of records, processing them all, and then committing new
offsets back to Kafka. This pattern is implemented in the `dgroomes.kafka_in_kafka_out.kafka_high_level_consumer.SyncConsumer`
class.

While simple, this pattern suffers from bottle-necking. You will eventually turn to other patterns to increase
throughput in systems that need it. This project implements a couple of "key-based" asynchronous consumer patterns that  
address this:

  * `dgroomes.kafka_in_kafka_out.kafka_high_level_consumer.KeyBasedAsyncConsumerWithVirtualThreads`
  * `dgroomes.kafka_in_kafka_out.kafka_high_level_consumer.KeyBasedAsyncConsumerWithCoroutines`


