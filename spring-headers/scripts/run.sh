# Run the app

JAR="$SPRING_HEADERS_ROOT_DIR/build/libs/spring-headers.jar"

if [[ ! -e "$JAR" ]]; then
  echo "$JAR does not exist. Build the project first before running." >&2
  exit 1;
fi

java -jar "$JAR"
