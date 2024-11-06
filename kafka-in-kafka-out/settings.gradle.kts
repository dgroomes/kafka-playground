rootProject.name = "kafka-in-kafka-out"

include("app",
    "kafka-consumer-synchronous",
    "kafka-consumer-with-coroutines",
    "kafka-consumer-with-virtual-threads",
    "test-harness")
