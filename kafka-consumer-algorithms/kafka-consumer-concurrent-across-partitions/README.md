# kafka-consumer-concurrent-across-partitions

A Kafka consumer that processes messages concurrently across partitions and decouples message processing from the poll loop.


## Overview

Depending on the workload, an asynchronous implementation can yield higher throughput and lower end-to-end latency than
a synchronous/sequential implementation. These are the key features:

* Message processing is concurrent
* Message processing related to one partition does not block message processing related to another partition
* Message processing does not block polling
* Message processing does not block offset committing
* Message processing is not confined to the records in a given poll batch
* Offset committing for one partition does not block offset committing for another partition

As with any correctly implemented Kafka consumer, messages in the same partition are processed in order and offsets are
committed when all messages up to that offset have been processed.
