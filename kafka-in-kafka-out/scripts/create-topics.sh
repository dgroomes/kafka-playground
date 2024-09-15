#!/usr/bin/env bash
# Create the Kafka topics.

set -eu

kafka-topics --create --bootstrap-server localhost:9092 --if-not-exists --partitions 2 --replication-factor 1 --topic input-text
kafka-topics --create --bootstrap-server localhost:9092 --if-not-exists --partitions 1 --replication-factor 1 --topic lowest-word
