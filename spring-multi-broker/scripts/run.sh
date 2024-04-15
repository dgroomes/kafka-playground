# Run the app

set -eu

START_SCRIPT="$SPRING_MULTI_BROKER_ROOT_DIR"/build/install/spring-multi-broker/bin/spring-multi-broker

if [[ ! -e "$START_SCRIPT" ]]; then
  echo "$START_SCRIPT does not exist. Build the project first before running." >&2
  exit 1;
fi

"$START_SCRIPT"
