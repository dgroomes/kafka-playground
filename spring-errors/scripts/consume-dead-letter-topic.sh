#!/usr/bin/env bash
# Consume from the dead-letter topic

kcat -C -b localhost:9092 -t my-messages.DLT
