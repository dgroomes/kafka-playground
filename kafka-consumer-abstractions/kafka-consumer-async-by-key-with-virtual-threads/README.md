# kafka-consumer-async-by-key-with-virtual-threads

An asynchronous Kafka consumer that processes messages in partition-key-offset order and is implemented with virtual threads.


## Overview

The semantics of a Kafka system are typically that the order of records in a partition is meaningful and that they
must be processed in *partition-offset* order. In many systems, we can make the more fine-grained assertion that only
messages in the same partition and that have the same *key* must be processed in order. This gives an extra degree of
freedom with regard to concurrent processing. We can process multiple messages in the same partition at the same time,
so long as they are for different keys. Depending on the workload, this can significantly increase throughput and reduce
latency.

Consider that we have a Kafka consumer that needs to consume banking transactions that are represented in these
messages:

```text
Partition 0:
[key:account_123] {"type": "DEPOSIT", "amount": 100}
[key:account_456] {"type": "DEPOSIT", "amount": 50}
[key:account_123] {"type": "WITHDRAW", "amount": 30}
```

```text
Partition 1:
[key:account_789] {"type": "DEPOSIT", "amount": 75}
[key:account_789] {"type": "WITHDRAW", "amount": 35}
```

In a typical Kafka consumer implementation, we would only be able to process at most two messages concurrently because
there are two partitions. Because we're doing a key-based implementation, we can process three message concurrently at
time zero: the $50 deposit for `account_123`, the $100 deposit for `account_456`, and the $75 deposit for `account_789`.
