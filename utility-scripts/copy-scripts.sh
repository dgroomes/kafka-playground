#!/usr/bin/env bash
# Copy the utility scripts to the other sub-projects

set -eu

subProjects=(
'interactive'
'connection-check'
'kafka-in-kafka-out'
'spring-barebones'
'spring-errors'
'spring-headers'
'spring-seekable'
'streams'
'streams-zip-codes'
'ssl-tls-security'
)

for i in "${subProjects[@]}"; do
    mkdir -p "../$i/scripts"
    cp log4j.properties "../$i/scripts"
    cp server.properties "../$i/scripts"
    cp start-kafka.sh "../$i/scripts"
    cp stop-kafka.sh "../$i/scripts"
done
