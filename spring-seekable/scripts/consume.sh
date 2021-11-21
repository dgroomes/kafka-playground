#!/usr/bin/env bash

# Consume from the Kafka topic
kcat -b localhost:9092 -t my-messages
