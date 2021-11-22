# Run the app

set -eu

JAR="$SPRING_MULTI_BROKER_ROOT_DIR/build/libs/spring-multi-broker.jar"

if [[ ! -e "$JAR" ]]; then
  echo "$JAR does not exist. Build the project first before running." >&2
  exit 1;
fi

java -jar "$JAR"
