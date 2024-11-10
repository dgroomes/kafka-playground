rootProject.name = "kafka-consumer-algorithms"

include("example-consumer-app",
    "kafka-consumer-sequential",
    "kafka-consumer-concurrent-across-partitions-within-same-poll",
    "kafka-consumer-concurrent-across-partitions",
    "kafka-consumer-concurrent-across-keys-with-coroutines",
    "kafka-consumer-concurrent-across-keys",
    "test-harness")
