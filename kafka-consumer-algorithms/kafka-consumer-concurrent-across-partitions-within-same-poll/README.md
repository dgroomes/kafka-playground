# kafka-consumer-concurrent-across-partitions-within-same-poll

A Kafka consumer that processes messages concurrently across partitions but is sequentially confined between polls.


## Overview

This consumer is part concurrent and part sequential. On each call to `Consumer#poll(Duration)`, the consumer processes
records concurrently across partitions, but these tasks are all joined before the next poll. So the consumer is
concurrent within a poll but sequential between polls.

This is a pretty good compromise between implementation complexity and performance.
