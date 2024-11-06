rootProject.name = "kafka-consumer-abstractions"

include("example-consumer-app",
    "kafka-consumer-synchronous",
    "kafka-consumer-with-coroutines",
    "kafka-consumer-with-virtual-threads",
    "test-harness")
