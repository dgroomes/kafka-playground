# Build (without the tests)

set -eu

# Bash trick to get the directory containing the script. See https://stackoverflow.com/a/246128
dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

cd "$dir/.."

./gradlew installDist -x test
