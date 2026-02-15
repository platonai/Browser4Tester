#!/bin/bash
#
# Self-Healing Test Orchestrator Runner
#
# Usage: run-healer.sh <target-project-path> <test-class1> [test-class2] ...
#
set -e

if [ $# -lt 2 ]; then
    echo "Usage: $0 <target-project-path> <test-class1> [test-class2] ..."
    exit 1
fi

TARGET_PROJECT="$1"
shift

# Get the healer jar path
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HEALER_JAR="$SCRIPT_DIR/../browser4-test-healer/target/browser4-test-healer-0.1.0-SNAPSHOT-jar-with-dependencies.jar"

if [ ! -f "$HEALER_JAR" ]; then
    echo "Healer jar not found: $HEALER_JAR"
    echo "Please run: mvn clean package -DskipTests"
    exit 1
fi

# Change to target project
cd "$TARGET_PROJECT"

# Build classpath: healer jar + target project test classpath
TARGET_CP=$(mvn dependency:build-classpath -DincludeScope=test -q | grep -v '^\[' | tail -1)
FULL_CP="$HEALER_JAR:target/classes:target/test-classes:$TARGET_CP"

echo "=== Self-Healing Test Orchestrator ==="
echo "Target Project: $TARGET_PROJECT"
echo "Test Classes: $@"
echo ""

# Run the healer
java -cp "$FULL_CP" com.browser4tester.healer.MainKt "$@"
