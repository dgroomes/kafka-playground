#!/usr/bin/env bash
# Copy the utility scripts to the other subprojects

set -eu

# Move to the directory containing this script so that the rest of this script can safely assume that the current working
# directory is the containing directory.
cd -- "$( dirname -- "${BASH_SOURCE[0]}" )"

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

# For the subproject at the path given by the first positional parameter, create a 'scripts/' directory and copy over
# the Kafka utility scripts and Kafka configuration files into it.
#
# For example, `copyScripts ../connection-check` or `copyScripts ../interactive`
copyScripts() {
  local subProjectDir="$1"

  if [[ ! -d "$subProjectDir" ]]; then
    echo >&2 "'$subProjectDir' is not a directory"
    exit 1
  fi

  local scriptsDir="$subProjectDir/scripts"
  mkdir -p "$scriptsDir"

  cp log4j.properties "$scriptsDir"
  cp server.properties "$scriptsDir"

  cp start-kafka.sh "$scriptsDir"
  cp stop-kafka.sh "$scriptsDir"
}

for i in "${subProjects[@]}"; do
  copyScripts "../$i"
done
