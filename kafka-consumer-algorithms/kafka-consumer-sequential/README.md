# kafka-consumer-sequential

This the most basic Kafka consumer pattern. It processes each record in sequence (one at a time).


## Overview

This is a sequential/synchronous consumer pattern. The consumer processes each record returned via a `Consumer#poll(Duration)`
invocation and then commits the offsets. This is a "process the batch to completion" style of consumer.
