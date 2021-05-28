#!/usr/bin/env bash

set -eu

# Consume from the city-level statistics output Kafka Streams topic
kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic streams-zip-codes-city-stats-changelog \
    --from-beginning \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.key=true \
    --property print.value=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
