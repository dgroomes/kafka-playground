# kafka-consumer-async

An asynchronous Kafka consumer that processes messages and commits offsets in partition order.


## Overview

This is an asynchronous Kafka consumer implementation. It is not a sequential consumer. Specifically, the processing of
records is not confined to the current poll batch. These are the key features:

* Messages within the same partition are processed sequentially, preserving order
* Different partitions can be processed at the same time
* Offset commits are managed asynchronously but maintain proper ordering
* Processing does not block the polling loop
