rootProject.name = "kafka-consumer-abstractions"

include("example-consumer-app",
    "kafka-consumer-sequential",
    "kafka-consumer-parallel-within-same-poll",
    "kafka-consumer-with-coroutines",
    "kafka-consumer-with-virtual-threads",
    "test-harness")
