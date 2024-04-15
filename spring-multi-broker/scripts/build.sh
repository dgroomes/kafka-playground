# Build (without the tests)

set -eu

"$SPRING_MULTI_BROKER_ROOT_DIR"/gradlew -p "$SPRING_MULTI_BROKER_ROOT_DIR" installDist -x test
