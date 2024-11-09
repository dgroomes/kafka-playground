rootProject.name = "kafka-consumer-algorithms"

include("example-consumer-app",
    "kafka-consumer-sequential",
    "kafka-consumer-parallel-within-same-poll",
    "kafka-consumer-async",
    "kafka-consumer-async-by-key-with-coroutines",
    "kafka-consumer-async-by-key-with-virtual-threads",
    "test-harness")
