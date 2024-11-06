# kafka-consumer-synchronous

When designing and operating a system that uses Kafka, you will encounter different techniques for consuming messages.
The most familiar pattern is synchronously polling a batch of records, processing them all, and then committing new
offsets back to Kafka. This pattern is implemented in this module.

While simple, this pattern suffers from bottle-necking. You will eventually turn to other patterns to increase
throughput in systems that need it. See the sibling modules for asynchronous-implementations that address the
bottle-necking:

* `kafka-consumer-with-coroutines`
* `kafka-consumer-with-virtual-threads`


