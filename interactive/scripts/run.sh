# Run the app

START_SCRIPT="$INTERACTIVE_ROOT_DIR"/build/install/interactive/bin/interactive

if [[ ! -e "$START_SCRIPT" ]]; then
  echo "$START_SCRIPT does not exist. Build the project first before running." >&2
  exit 1;
fi

"$START_SCRIPT"
