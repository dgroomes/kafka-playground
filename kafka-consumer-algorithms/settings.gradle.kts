rootProject.name = "kafka-consumer-algorithms"

include(
    "kafka-consumer-sequential",
    "kafka-consumer-concurrent-across-partitions-within-same-poll",
    "kafka-consumer-concurrent-across-partitions",
    "kafka-consumer-concurrent-across-keys-with-coroutines",
    "kafka-consumer-concurrent-across-keys",
    "runner"
)
