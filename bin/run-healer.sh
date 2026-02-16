#!/bin/bash
#
# Self-Healing Test Orchestrator Runner
#
# Usage: 
#   run-healer.sh <target-project-path> [test-class1] [test-class2] ...
#
# If no test classes specified, discovers all modules and test classes automatically
#
set -e

# Get the healer jar path
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HEALER_JAR="$SCRIPT_DIR/../browser4-test-healer/target/browser4-test-healer-0.1.0-SNAPSHOT-jar-with-dependencies.jar"

if [ ! -f "$HEALER_JAR" ]; then
    echo "❌ Healer jar not found: $HEALER_JAR"
    echo "Please run: mvn clean package -DskipTests"
    exit 1
fi

# Parse arguments
if [ $# -lt 1 ]; then
    echo "Usage: $0 <target-project-path> [test-class1] [test-class2] ..."
    echo ""
    echo "If no test classes specified, all modules and tests will be discovered automatically"
    exit 1
fi

TARGET_PROJECT="$1"
shift

if [ ! -d "$TARGET_PROJECT" ]; then
    echo "❌ Target project not found: $TARGET_PROJECT"
    exit 1
fi

# Change to target project
cd "$TARGET_PROJECT"

echo "=== Self-Healing Test Orchestrator ==="
echo "Target Project: $(pwd)"
echo ""

# Function to discover all test classes in a module
discover_test_classes() {
    local module_path="$1"
    local test_dir="$module_path/src/test"
    
    if [ ! -d "$test_dir" ]; then
        return
    fi
    
    # Find all test files (Kotlin and Java)
    find "$test_dir" -type f \( -name "*Test*.kt" -o -name "*Test*.java" \) | while read -r test_file; do
        # Extract package and class name
        local rel_path="${test_file#$test_dir/}"
        rel_path="${rel_path#kotlin/}"
        rel_path="${rel_path#java/}"
        
        # Convert path to fully qualified class name
        local fqcn="${rel_path%.kt}"
        fqcn="${fqcn%.java}"
        fqcn="${fqcn//\//.}"
        
        echo "$fqcn"
    done
}

# Function to run healer for a specific module
run_module_tests() {
    local module_path="$1"
    local module_name=$(basename "$module_path")
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📦 Module: $module_name"
    echo "   Path: $module_path"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    # Check if module has tests
    if [ ! -d "$module_path/src/test" ]; then
        echo "⏭️  No tests found, skipping..."
        echo ""
        return 0
    fi
    
    # Compile the module first
    echo "🔨 Compiling module..."
    cd "$module_path"
    if ! mvn test-compile -q -DskipTests 2>&1 | grep -v "^\[INFO\]" | grep -v "^Download" | head -20; then
        echo "⚠️  Compilation issues detected, continuing..."
    fi
    
    # Discover test classes
    echo "🔍 Discovering test classes..."
    local test_classes=$(discover_test_classes "$module_path")
    
    if [ -z "$test_classes" ]; then
        echo "⏭️  No test classes found, skipping..."
        echo ""
        return 0
    fi
    
    local test_count=$(echo "$test_classes" | wc -l)
    echo "✓ Found $test_count test class(es)"
    echo ""
    
    # Build classpath for this module
    local module_cp=$(mvn dependency:build-classpath -DincludeScope=test -q 2>/dev/null | grep -v '^\[' | tail -1)
    local full_cp="$HEALER_JAR:target/classes:target/test-classes:$module_cp"
    
    # Run healer for all test classes at once
    local test_classes_array=($test_classes)
    echo "  🧪 Testing ${#test_classes_array[@]} classes..."
    
    # Create a temporary file to capture output
    local temp_output="/tmp/healer-module-$$.log"
    
    if java -cp "$full_cp" com.browser4tester.healer.MainKt ${test_classes_array[@]} 2>&1 | tee "$temp_output" | grep -q "All classes passed"; then
        echo "     ✅ All tests passed"
        local result=0
    else
        echo "     ❌ Some tests failed or unstable"
        # Extract failed classes
        grep "^- " "$temp_output" | sed 's/^/     /'
        local result=1
    fi
    
    rm -f "$temp_output"
    echo ""
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Module Summary: $module_name"
    if [ $result -eq 0 ]; then
        echo "✅ All tests passed"
    else
        echo "⚠️  Some tests failed"
    fi
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    cd "$TARGET_PROJECT"
    return $result
}

# Main execution logic
if [ $# -gt 0 ]; then
    # Manual mode: specific test classes provided
    echo "🎯 Mode: Manual (specific test classes)"
    echo "Test Classes: $@"
    echo ""
    
    # Build classpath
    TARGET_CP=$(mvn dependency:build-classpath -DincludeScope=test -q 2>/dev/null | grep -v '^\[' | tail -1)
    FULL_CP="$HEALER_JAR:target/classes:target/test-classes:$TARGET_CP"
    
    # Run the healer with specified classes
    java -cp "$FULL_CP" com.browser4tester.healer.MainKt "$@"
else
    # Auto mode: discover all modules and tests
    echo "🤖 Mode: Auto (discovering all modules and tests)"
    echo ""
    
    # Get module list in dependency order using Maven reactor
    echo "🔍 Discovering modules in dependency order..."
    MODULES=$(mvn -q exec:exec -Dexec.executable=pwd -Dexec.workingdir='${project.basedir}' 2>/dev/null | grep -v "^\[" | sort -u)
    
    if [ -z "$MODULES" ]; then
        # Fallback: try to find pom.xml files
        echo "⚠️  Maven reactor failed, using fallback discovery..."
        if [ -f "pom.xml" ]; then
            # Check if it's a multi-module project
            if grep -q "<modules>" pom.xml; then
                # Extract module names from parent pom
                MODULES=$(grep -A 100 "<modules>" pom.xml | grep "<module>" | sed 's/.*<module>\(.*\)<\/module>.*/\1/' | while read module; do
                    echo "$(pwd)/$module"
                done)
            else
                # Single module project
                MODULES="$(pwd)"
            fi
        else
            echo "❌ No pom.xml found in target project"
            exit 1
        fi
    fi
    
    # Count modules
    MODULE_COUNT=$(echo "$MODULES" | wc -l)
    echo "✓ Found $MODULE_COUNT module(s)"
    echo ""
    
    # Process each module in order
    MODULE_NUM=1
    GLOBAL_FAILED=""
    GLOBAL_PASSED=0
    GLOBAL_FAILED_COUNT=0
    
    while read -r module_path; do
        if [ -f "$module_path/pom.xml" ]; then
            echo ""
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            echo "Processing Module $MODULE_NUM of $MODULE_COUNT"
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            
            # Run module tests and capture result
            if run_module_tests "$module_path"; then
                ((GLOBAL_PASSED++)) || true
            else
                ((GLOBAL_FAILED_COUNT++)) || true
                GLOBAL_FAILED="$GLOBAL_FAILED\n  - $(basename $module_path)"
            fi
            
            MODULE_NUM=$((MODULE_NUM + 1))
        fi
    done <<< "$MODULES"
    
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "🎉 All modules processed!"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Total: $MODULE_COUNT modules"
    echo "Passed: $GLOBAL_PASSED modules"
    echo "Failed: $GLOBAL_FAILED_COUNT modules"
    if [ -n "$GLOBAL_FAILED" ]; then
        echo ""
        echo "Failed modules:$GLOBAL_FAILED"
    fi
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
fi
