# kafka-consumer-concurrent-across-keys

A Kafka consumer that processes messages concurrently across partition-key groups.


## Overview

The semantics of a Kafka system are typically that the order of records in a partition is meaningful and that they
must be processed in *partition-offset* order. This is a well-understood constraint. In many systems, we can make a
more fine-grained assertion that only messages in the same partition and that have the same *key* must be processed
in order.

A partition-key-offset ordering constraint is more relaxed than the partition-offset constraint, and it gives us an
extra degree of freedom with regard to concurrent processing. We can process multiple messages in the same partition at
the same time, so long as they are for different keys. Depending on the workload, this can significantly increase
throughput and reduce end-to-end latency.

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
