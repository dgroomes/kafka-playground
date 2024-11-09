# kafka-consumer-parallel-within-same-poll

A sequential consumer with some parallelization. For records returned by a poll, records are processed with parallelism equal to the number of partitions.


## Overview

This is a sequential/synchronous consumer pattern because it processes each record returned via a `Consumer#poll(Duration)`
invocation and then commits the offsets. But, within a given poll batch, each partition-group of records is processed in
parallel. This greatly increases throughput on multicore machines.
