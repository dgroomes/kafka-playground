# Run the app

START_SCRIPT="$SEEKABLE_KAFKA_ROOT_DIR"/build/install/spring-seekable/bin/spring-seekable

if [[ ! -e "$START_SCRIPT" ]]; then
  echo "$START_SCRIPT does not exist. Build the project first before running." >&2
  exit 1;
fi

"$START_SCRIPT"
