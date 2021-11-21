#!/usr/bin/env bash
# Produce a test JSON message to the `my-messages` Kafka topic that has a field of the wrong type

set -eu

SECOND=$(date -j +%-S)

echo "{\"message\": \"hello\", \"time\": \"${SECOND}ishouldbeanumber\"}" | kcat -P -b localhost:9092 -t my-messages
