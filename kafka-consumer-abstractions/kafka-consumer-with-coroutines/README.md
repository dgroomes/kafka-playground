# kafka-consumer-with-coroutines

An asynchronous Kafka consumer and message processor implemented with coroutines.

Messages are processed in "key order". This means that a message with a key of "xyz" will always be processed before
a later message with a key of "xyz". Messages with the same key are assumed to be on the same partition.

Notice: This implementation does not do error handling, it is not careful about shutdown semantics, and it does not
handle Kafka consumer group re-balances and other cases. A production implementation must take more care.

The logical algorithm is designed for high bandwidth and low latency. Specifically, we've designed for these traits:

* Message processing is concurrent
* Message processing related to one key does not block message processing related to another key
* Message processing does not block polling
* Message processing does not block offset committing
* Message processing is not confined to the records in a given poll batch
* Offset committing for one partition does not block offset committing for another partition

In contrast to the logic, the actual implementation is not necessarily optimized for low latency because it allocates
lots of objects and I think spends a lot of cycles on context switching. But that sort of optimization was not my
goal. My main goal was to explore the expressiveness of the coroutines programming model. The end result I think is
pretty expressive.
