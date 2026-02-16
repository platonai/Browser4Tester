# Browser4 Test Healer

A self-healing test orchestrator with persistent DAG-based tracking for JUnit 5 Kotlin tests.

## What is This?

Browser4 Test Healer is an intelligent test orchestrator that:

1. **Executes tests class-by-class** using JUnit Platform Launcher API
2. **Automatically fixes failing tests** using GitHub Copilot AI
3. **Tracks complete history** of all test executions and AI repairs
4. **Maintains a DAG** (Directed Acyclic Graph) of your test structure
5. **Persists everything** to JSON for analysis and auditing

## Key Features

### 🔄 Self-Healing
- Detects test failures automatically
- Invokes GitHub Copilot CLI to generate fixes
- Applies patches and re-runs tests
- Retries up to 3 times per class
- Git-safe with automatic snapshots and rollback

### 📊 Persistent DAG Tracking
- Builds complete test dependency graph
- Tracks module/class/method hierarchy
- Records execution history (last 50 per method)
- Records remediation history (last 20 per method)
- Preserves history across graph rebuilds

### 🗂️ Dedicated Workspaces
- Each AI fix gets its own folder
- Saves failure context, prompts, responses
- Complete audit trail for compliance
- Easy to review AI decisions

### 🛡️ Safety First
- Only modifies test code by default
- Integrity guard prevents test degradation
- Git snapshots before every fix
- Automatic rollback on failure

## Quick Start

### Prerequisites

- Java 17 or later
- Maven 3.8+
- GitHub Copilot subscription
- GitHub CLI (`gh`) installed and authenticated

### Installation

```bash
# Clone the repository
git clone https://github.com/platonai/Browser4Tester.git
cd Browser4Tester/browser4-test-healer

# Build
mvn clean package

# The fat JAR is at:
# target/browser4-test-healer-0.1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Basic Usage

```bash
# Run with persistent tracking
java -cp target/browser4-test-healer-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
  com.browser4tester.healer.MainWithPersistenceKt \
  --project-root /path/to/your/project \
  com.example.TestClass1 \
  com.example.TestClass2

# Run without persistence (faster, no history)
java -cp target/browser4-test-healer-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
  com.browser4tester.healer.MainKt \
  com.example.TestClass1 \
  com.example.TestClass2
```

## Architecture

### Core Components

```
TestOrchestrator              → Basic orchestration (no persistence)
PersistentTestOrchestrator    → Full DAG tracking + history
├── ClassExecutor             → Executes tests via JUnit Platform
├── CopilotAgent              → Integrates with GitHub Copilot CLI
├── PatchApplier              → Applies code changes
├── GitSnapshotManager        → Git safety net
├── TestIntegrityGuard        → Prevents test degradation
├── PersistenceManager        → JSON storage
├── TestGraphBuilder          → Discovers tests
├── ExecutionHistoryManager   → Tracks executions
├── RemediationHistoryManager → Tracks AI fixes
└── RemediationWorkspace      → Manages fix workspaces
```

### Data Model

```
TestGraph (DAG)
├── ModuleNode (Maven module)
│   ├── TestClassNode (Test class)
│   │   ├── TestMethodNode (Test method)
│   │   │   ├── ExecutionRecord (history)
│   │   │   └── RemediationRecord (history)
```

### Persistence Structure

```
.browser4tester/
├── test-graph.json           # Current test graph
├── archives/                 # Historical snapshots
│   └── test-graph-*.json
└── remediation/              # Fix workspaces
    └── {Class}_{Method}_{Timestamp}/
        ├── README.md
        ├── failure-context.txt
        ├── copilot-prompt.txt
        ├── copilot-response.txt
        ├── diagnostic-report.md
        ├── applied-changes/
        └── logs/
```

## How It Works

### Basic Flow

```
1. Load/Build Test Graph
   ├─> Check if graph exists
   ├─> Apply update strategy
   └─> Merge history if rebuilding

2. Execute Tests Class-by-Class
   ├─> Run test class
   ├─> Record execution for all methods
   └─> If failed:
       ├─> Create remediation workspace
       ├─> Save failure context
       ├─> Invoke GitHub Copilot
       ├─> Apply fix
       ├─> Record remediation
       └─> Re-run (up to 3 attempts)

3. Save Updated Graph
   ├─> Write to test-graph.json
   └─> Archive snapshot
```

### Graph Update Strategies

The orchestrator uses smart strategies to decide when to rebuild the test graph:

- **Default**: Rebuild if pom.xml modified OR graph older than 24 hours
- **Always**: Rebuild on every run (slowest, always fresh)
- **Never**: Only build if doesn't exist (fastest, may be stale)
- **TimeBasedStrategy**: Rebuild after time expires
- **FileModificationStrategy**: Rebuild if test files modified
- **PomModificationStrategy**: Rebuild if pom.xml modified

## Testing

The module includes comprehensive tests:

```bash
# Run all tests
mvn test

# Results:
# - ExecutionHistoryManagerTest (4 tests)
# - RemediationHistoryManagerTest (6 tests)
# - PersistenceManagerTest (5 tests)
# - RemediationWorkspaceTest (8 tests)
# - TestIntegrityGuardTest (2 tests)
# - PersistentTestOrchestratorTest (6 tests)
# Total: 31 tests, all passing ✅
```

## Configuration

### OrchestratorConfig

```kotlin
OrchestratorConfig(
    maxRetryPerClass = 3,              // Max AI fix attempts
    allowMainSourceEdits = false,      // Allow modifying production code
    testRoot = Path.of(".")           // Test source root
)
```

### History Limits

```kotlin
// In ExecutionHistoryManager
private val maxHistoryPerMethod = 50  // Last 50 executions

// In RemediationHistoryManager
private val maxHistoryPerMethod = 20  // Last 20 remediations
```

## Safety Features

### 1. Test Integrity Guard

Prevents AI from degrading test quality:

```kotlin
class TestIntegrityGuard {
    fun verify(beforeFix: String, afterFix: String) {
        // Blocks:
        // - Removal of test methods
        // - Removal of assertions
        // - Reduction in assertion count
    }
}
```

### 2. Git Snapshots

Automatic snapshots before every fix:

```kotlin
gitSnapshotManager.snapshot("pre-ai-fix snapshot for $class")
// ... apply fix ...
if (!repaired) {
    gitSnapshotManager.rollbackLastCommit()
}
```

### 3. Test-Only Modifications

By default, only test code can be modified:

```kotlin
OrchestratorConfig(
    allowMainSourceEdits = false  // Production code is safe
)
```

## Performance

### Benchmarks (typical project with 1000 tests)

| Operation | Time |
|-----------|------|
| Cold start (build graph) | 5-30s |
| Cached graph load | 100-500ms |
| Record execution | < 1ms |
| Record remediation | < 5ms |
| Save graph | 100-500ms |
| Memory usage | < 10MB |

### Storage

| Item | Size |
|------|------|
| Graph data | ~1KB per test method |
| 1000 tests | ~1MB JSON |
| Archives | Grows over time |

## Best Practices

### 1. Git Ignore

```gitignore
# Add to .gitignore
.browser4tester/
```

### 2. Workspace Cleanup

```bash
# Delete workspaces older than 30 days
find .browser4tester/remediation -type d -mtime +30 -exec rm -rf {} \;
```

### 3. CI/CD Integration

- **Local Development**: Use `PersistentTestOrchestrator` for full tracking
- **CI Builds**: Use `TestOrchestrator` for speed (no persistence)

### 4. Monitoring

Use history data to identify problems:

```kotlin
// Find flaky tests
val flakyTests = graph.modules
    .flatMap { it.testClasses }
    .flatMap { it.testMethods }
    .filter { method ->
        val stats = executionManager.getExecutionStats(method)
        stats.successRate in 0.2..0.8 && stats.totalExecutions >= 10
    }
```

## Documentation

- **[DESIGN.md](../DESIGN.md)** - Complete architecture and design decisions
- **[USAGE_GUIDE.md](USAGE_GUIDE.md)** - Detailed usage examples and API reference
- **[EXAMPLES.md](EXAMPLES.md)** - Real-world examples and data queries
- **[IMPLEMENTATION_NOTES.md](../IMPLEMENTATION_NOTES.md)** - Implementation challenges and solutions

## Limitations

1. **JUnit 5 Only**: Currently supports JUnit 5 (Jupiter) tests
2. **Kotlin/Java**: Designed for Kotlin, works with Java
3. **Maven Projects**: Assumes Maven project structure
4. **GitHub Copilot**: Requires GitHub Copilot subscription
5. **Single-threaded**: Executes tests sequentially (parallel execution is future work)

## Future Enhancements

- [ ] Parallel test execution using DAG
- [ ] Support for TestNG, Spock
- [ ] Gradle project support
- [ ] Web dashboard for visualization
- [ ] ML-based flaky test prediction
- [ ] Cost tracking for Copilot API usage
- [ ] Integration with CI/CD platforms
- [ ] Trend analysis and reporting

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Add tests for new features
4. Ensure all tests pass: `mvn test`
5. Submit a pull request

## License

See the main repository for license information.

## Support

- **Issues**: [GitHub Issues](https://github.com/platonai/Browser4Tester/issues)
- **Discussions**: [GitHub Discussions](https://github.com/platonai/Browser4Tester/discussions)
- **Documentation**: See `DESIGN.md` and `USAGE_GUIDE.md`

## Credits

Developed as part of the Browser4Tester project by PlatonAI.

---

**Note**: This is an experimental tool. Always review AI-generated code changes before committing to production.
