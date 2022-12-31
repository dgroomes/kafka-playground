#!/usr/bin/env bash
# Copy the utility scripts to the other sub-projects

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

if ! command -v brew &> /dev/null
then
  echo >&2 "Please ensure that HomeBrew is installed. The 'brew' command could not be found."
  exit
fi

if ! brew list | grep '^kafka$' &> /dev/null
then
  echo >&2 "Please ensure that Kafka is installed via HomeBrew. 'kafka' is not listed in the HomeBrew-installed packages."
  exit
fi

KAFKA_CONFIG_DIR="$(brew --prefix kafka)/.bottle/etc/kafka"

if [ ! -d "$KAFKA_CONFIG_DIR" ]
then
  echo >&2 "The configuration directory for the HomeBrew-installed Kafka does not exist. Expected to find the directory '$KAFKA_CONFIG_DIR' but it does not exist."
  exit
fi

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

  # The default KRaft-mode configuration file (kraft/server.properties) provided by the Kafka installation is mostly
  # exactly what we want, but we need to make a change to it. By default, it sets 'log.dirs=/opt/homebrew/var/lib/kraft-combined-logs'
  # but we would prefer to use a directory that is specific to the project in which we are using Kafka. So, we override
  # this property with the relative path 'tmp-kafka-data-logs'. The advantage of having a separate 'log.dirs' directory
  # for each project is that it makes it easier to delete the data for a project without affecting the data for other
  # projects, and it makes it easier to see the data for a project without having to sift through the data for other
  # projects.
  sed 's|^log\.dirs.*|log\.dirs=tmp-kafka-data-logs|' "$KAFKA_CONFIG_DIR/kraft/server.properties" > "$scriptsDir/server.properties"

  cp start-kafka.sh "$scriptsDir"
  cp stop-kafka.sh "$scriptsDir"
}

for i in "${subProjects[@]}"; do
  copyScripts "../$i"
done
