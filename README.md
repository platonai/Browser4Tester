# Browser4Tester

🤖 **Self-Healing Test Orchestrator** - Automatically repairs failing tests using GitHub Copilot CLI

## Overview

Browser4Tester implements a class-level self-healing test orchestrator for Kotlin/JUnit 5 projects. When tests fail, it automatically invokes GitHub Copilot to analyze and fix the issues.

## Features

✅ **Auto-Discovery Mode** - Automatically find all modules and tests  
✅ **Dependency-Ordered Execution** - Use Maven reactor for correct module order  
✅ **Class-Level Isolation** - Each test class executes independently  
✅ **Intelligent Failure Detection** - Captures method-level failures with full stack traces  
✅ **AI-Powered Repair** - Leverages GitHub Copilot CLI for automatic fixes  
✅ **Integrity Protection** - Prevents weakening tests (e.g., removing assertions)  
✅ **Git Safety** - Automatic rollback if repairs fail  
✅ **Retry Logic** - Configurable retry attempts per class (default: 3)  
✅ **Batch Processing** - Process all tests in a module together for efficiency

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.6+
- GitHub CLI with Copilot extension (`gh copilot`)

### Build

```bash
mvn clean package -DskipTests
```

### Usage

#### Auto Mode (Recommended)

Automatically discover and test all modules and test classes:

```bash
# Test entire project
./bin/run-healer.sh /path/to/project

# Test single module
./bin/run-healer.sh /path/to/project/specific-module
```

The script will:
- 🔍 Discover all modules in dependency order (using Maven reactor)
- 📦 Process each module sequentially
- 🧪 Find all test classes in each module
- 🤖 Run self-healing tests on all discovered classes
- 📊 Generate summary report

#### Manual Mode

Test specific classes:

```bash
./bin/run-healer.sh /path/to/project com.example.Test1 com.example.Test2
```

### Tested On

✅ Successfully tested on `Browser4-4.6` project
- Multi-module Maven project with 11+ modules
- Automatic module discovery in dependency order
- Batch testing of all modules and test classes
- Module: `pulsar-core/pulsar-dom` - 2 test classes
- Module: `pulsar-core/pulsar-browser` - 14 test classes
- Module: `pulsar-core/pulsar-common` - 3 test classes
- Status: Auto-discovery working correctly

## Architecture

```
┌─────────────────────┐
│ TestOrchestrator    │  Core execution coordinator
├─────────────────────┤
│ ClassExecutor       │  JUnit Platform Launcher integration
│ CopilotAgent        │  GitHub Copilot CLI wrapper
│ PatchApplier        │  File modification handler
│ GitSnapshotManager  │  Version control safety
│ TestIntegrityGuard  │  Quality assurance
└─────────────────────┘
```

## Safety Mechanisms

1. **Assertion Count Guard** - Prevents reduction in assertion count
2. **Test Method Guard** - Prevents deletion of @Test methods
3. **Tautology Detection** - Blocks meaningless assertions like `assertTrue(true)`
4. **Git Snapshots** - Automatic commit before repairs
5. **Rollback on Failure** - Reverts changes if repair fails after max retries

## Configuration

Edit `OrchestratorConfig` in your main class:

```kotlin
OrchestratorConfig(
    maxRetryPerClass = 3,              // Max repair attempts
    allowMainSourceEdits = false,      // Restrict to test files only
    testRoot = Path.of(".")            // Test root directory
)
```

## How It Works

1. **Execute** - Run test class via JUnit Platform Launcher
2. **Detect** - Collect failure details (methods, messages, stack traces)
3. **Repair** - Call GitHub Copilot with failure context
4. **Validate** - Check test integrity (assertions, methods)
5. **Apply** - Write fixed code and stage changes
6. **Retry** - Re-run test class
7. **Rollback** - Revert if still failing after max retries

## Output Format

### Auto Mode
```
=== Self-Healing Test Orchestrator ===
Target Project: /path/to/project

🤖 Mode: Auto (discovering all modules and tests)

🔍 Discovering modules in dependency order...
✓ Found 5 module(s)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Processing Module 1 of 5
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📦 Module: common
   Path: /path/to/project/common
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔨 Compiling module...
🔍 Discovering test classes...
✓ Found 10 test class(es)

  🧪 Testing 10 classes...
     ✅ All tests passed

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Module Summary: common
✅ All tests passed
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[... more modules ...]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎉 All modules processed!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total: 5 modules
Passed: 4 modules
Failed: 1 modules

Failed modules:
  - api-module
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Manual Mode
```
=== Self-Healing Test Orchestrator ===
Target Project: /path/to/project

🎯 Mode: Manual (specific test classes)
Test Classes: com.example.MyTest

All classes passed.
```

Or on failure:

```
Unstable classes:
- com.example.FailingTest
```

## Limitations

- Requires GitHub Copilot CLI access
- Works with Kotlin/JUnit 5 tests only
- Test file must follow standard Maven/Gradle layout
- Copilot's fix quality depends on failure context clarity

## Contributing

This is an MVP implementation. Contributions welcome for:
- Additional test framework support (TestNG, Spock)
- Java test support
- Better Copilot output parsing
- Parallel test execution
- Enhanced integrity guards

## License

See LICENSE file

## Author

Built as a proof-of-concept for AI-driven test maintenance
