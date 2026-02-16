# Persistent DAG-Based Test Tracking - Usage Guide

## Overview

The persistent DAG-based test orchestrator provides a complete, self-healing test execution system with full historical tracking. It builds and maintains a Directed Acyclic Graph (DAG) of your test structure, tracks every execution and AI remediation attempt, and persists all data for analysis and auditing.

## Quick Start

### Basic Usage

```bash
# Build the fat JAR
cd browser4-test-healer
mvn clean package

# Run with persistent tracking
java -cp target/browser4-test-healer-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
  com.browser4tester.healer.MainWithPersistenceKt \
  --project-root /path/to/your/project \
  com.example.TestClass1 \
  com.example.TestClass2
```

### Using the Original Orchestrator

```bash
# Run without persistence (faster, no history)
java -cp target/browser4-test-healer-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
  com.browser4tester.healer.MainKt \
  com.example.TestClass1 \
  com.example.TestClass2
```

## Features

### 1. Automatic Test Discovery

The orchestrator automatically discovers:
- **Modules**: All Maven modules in your project
- **Test Classes**: All test classes containing JUnit 5 tests
- **Test Methods**: Individual test methods using JUnit Platform API

```kotlin
val graphBuilder = TestGraphBuilder(projectRoot)
val graph = graphBuilder.buildGraph()
// Graph contains complete test structure
```

### 2. Execution History Tracking

Every test execution is recorded with:
- Timestamp
- Result (SUCCESS, FAILURE, ERROR, SKIPPED)
- Duration in milliseconds
- Error message and stack trace
- Maven log paths

```kotlin
val executionHistory = methodNode.executionHistory
// Last 50 executions preserved
```

### 3. Remediation History Tracking

Every AI fix attempt is recorded with:
- Timestamp
- Result (SUCCESS, FAILURE, PARTIAL, SKIPPED)
- Diagnostic report
- Workspace path (dedicated folder)
- Copilot prompt and response
- List of modified files
- Duration

```kotlin
val remediationHistory = methodNode.remediationHistory
// Last 20 remediation attempts preserved
```

### 4. Dedicated Remediation Workspaces

Each AI fix attempt gets its own workspace:

```
.browser4tester/remediation/{ClassName}_{MethodName}_{Timestamp}/
├── README.md                    # Workspace documentation
├── failure-context.txt          # Test failure details
├── copilot-prompt.txt          # AI prompt sent
├── copilot-response.txt        # AI response received
├── diagnostic-report.md        # AI diagnostic analysis
├── applied-changes/            # Modified files
│   └── TestClass.kt
└── logs/
    └── activity.log            # Remediation activity log
```

Benefits:
- Complete audit trail for compliance
- Easy to review AI decisions
- Pattern analysis across repairs
- Self-documenting workspaces

### 5. Smart Graph Updates

The orchestrator uses configurable update strategies to determine when to rebuild the test graph:

**Default Strategy** (PomModification OR TimeBasedStrategy):
```kotlin
// Rebuild if pom.xml modified OR graph older than 24 hours
val DEFAULT_UPDATE_STRATEGY = GraphUpdateStrategy.CompositeStrategy(
    listOf(
        GraphUpdateStrategy.PomModificationStrategy(),
        GraphUpdateStrategy.TimeBasedStrategy(Duration.ofHours(24))
    )
)
```

**Other Strategies:**
- `Always`: Rebuild on every run (slowest, always fresh)
- `Never`: Only build if doesn't exist (fastest, may be stale)
- `TimeBasedStrategy(duration)`: Rebuild after time expires
- `FileModificationStrategy`: Rebuild if test files modified
- `PomModificationStrategy`: Rebuild if pom.xml modified
- `CompositeStrategy(strategies)`: Combine multiple strategies

### 6. History Preservation on Graph Rebuild

When the graph is rebuilt (e.g., after adding new tests), execution and remediation history is preserved:

```kotlin
private fun mergeGraphHistory(newGraph: TestGraph, oldGraph: TestGraph): TestGraph {
    // Matches methods by ID and transfers history
    // New methods start with empty history
    // Deleted methods' history is archived
}
```

## Persistence Structure

All data is stored in `.browser4tester/`:

```
.browser4tester/
├── test-graph.json              # Current test graph
├── archives/                    # Historical snapshots
│   ├── test-graph-2026-02-16T10-30-00.json
│   └── test-graph-2026-02-15T14-20-00.json
└── remediation/                 # Remediation workspaces
    ├── TestLogin_shouldAuthenticateUser_2026-02-16T10-30-00/
    └── TestCheckout_shouldProcessPayment_2026-02-16T11-00-00/
```

### Storage Format: JSON

**Why JSON?**
- Human-readable (inspect with text editor)
- Version-controllable (can track in git if desired)
- Language-agnostic (consumable by other tools)
- Good ecosystem support (Jackson)

**Typical Sizes:**
- ~1KB per test method (with history)
- 1000 tests ≈ 1MB JSON file
- Archives grow over time (consider periodic cleanup)

## Performance Characteristics

### Graph Building
- **Cold start** (first run): ~5-30 seconds for large projects
- **Cached** (using existing graph): ~100-500ms
- **Update strategy** minimizes unnecessary rebuilds

### Memory Usage
- **Graph in memory**: < 10MB for large projects (1000+ tests)
- **History windows**: Bounded by configuration (50 executions, 20 remediations)

### Execution Overhead
- **Recording execution**: < 1ms per test
- **Recording remediation**: < 5ms per attempt
- **Saving graph**: 100-500ms

## Configuration

### OrchestratorConfig

```kotlin
data class OrchestratorConfig(
    val maxRetryPerClass: Int = 3,              // Max AI fix attempts
    val allowMainSourceEdits: Boolean = false,  // Allow modifying production code
    val testRoot: Path = Path.of(".")          // Test source root
)
```

### History Limits

Modify in the managers:

```kotlin
class ExecutionHistoryManager {
    private val maxHistoryPerMethod = 50  // Keep last 50 executions
}

class RemediationHistoryManager {
    private val maxHistoryPerMethod = 20  // Keep last 20 remediations
}
```

## Integration Examples

### Programmatic Usage

```kotlin
// Create orchestrator
val orchestrator = PersistentTestOrchestrator(
    projectRoot = Path.of("/path/to/project"),
    classExecutor = ClassExecutor(),
    copilotAgent = CopilotAgent(),
    patchApplier = PatchApplier(),
    gitSnapshotManager = GitSnapshotManager(),
    guard = TestIntegrityGuard(),
    config = OrchestratorConfig(maxRetryPerClass = 3)
)

// Run tests
val result = orchestrator.run(listOf(
    "com.example.TestLogin",
    "com.example.TestCheckout"
)) { className ->
    // Convert class name to file path
    val relative = "src/test/kotlin/" + className.replace('.', '/') + ".kt"
    projectRoot.resolve(relative)
}

// Check results
if (result.allPassed) {
    println("All tests passed!")
} else {
    println("Failed classes:")
    result.classResults
        .filterNot { it.passed }
        .forEach { println("- ${it.className}") }
}

println("Remediation stats:")
println("- Total attempts: ${result.totalRemediationAttempts}")
println("- Successful: ${result.successfulRemediations}")
```

### Custom Update Strategy

```kotlin
// Only rebuild if pom.xml modified
val customStrategy = GraphUpdateStrategy.PomModificationStrategy()

val orchestrator = PersistentTestOrchestrator(
    projectRoot = projectRoot,
    // ... other parameters ...
    updateStrategy = customStrategy
)
```

### Accessing History Data

```kotlin
// Load graph
val persistenceManager = PersistenceManager()
val graph = persistenceManager.loadGraph()

// Find a specific method
val method = persistenceManager.findMethodNode(
    graph = graph!!,
    moduleId = "browser4-test-healer",
    classId = "com.browser4tester.healer.TestLogin",
    methodId = "testMethod"
)

// Get execution statistics
val executionManager = ExecutionHistoryManager()
val stats = executionManager.getExecutionStats(method!!)
println("Success rate: ${stats.successRate * 100}%")
println("Avg duration: ${stats.averageDurationMs}ms")

// Get remediation statistics
val remediationManager = RemediationHistoryManager()
val remediationStats = remediationManager.getRemediationStats(method)
println("Remediation attempts: ${remediationStats.totalAttempts}")
println("Success rate: ${remediationStats.successRate * 100}%")
```

## Best Practices

### 1. Version Control

**Do NOT commit** `.browser4tester/` to version control:

```gitignore
# Add to .gitignore
.browser4tester/
```

Reason: Contains execution logs, temporary workspaces, and potentially sensitive data.

**Exception**: You may choose to commit `test-graph.json` if you want to track test structure changes over time.

### 2. CI/CD Integration

**Development Mode** (with persistence):
```bash
# Use for local development and debugging
./gradlew test --use-persistent-orchestrator
```

**CI Mode** (without persistence):
```bash
# Use for fast CI builds
./gradlew test
```

### 3. Workspace Management

Workspaces grow over time. Consider periodic cleanup:

```bash
# Delete workspaces older than 30 days
find .browser4tester/remediation -type d -mtime +30 -exec rm -rf {} \;
```

Or implement automated cleanup:

```kotlin
fun cleanupOldWorkspaces(maxAgeDays: Int = 30) {
    val workspace = RemediationWorkspace()
    val cutoff = Instant.now().minus(maxAgeDays.toLong(), ChronoUnit.DAYS)
    
    workspace.listWorkspaces().forEach { workspacePath ->
        val timestamp = extractTimestamp(workspacePath)
        if (timestamp.isBefore(cutoff)) {
            Files.walk(workspacePath)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.delete(it) }
        }
    }
}
```

### 4. Monitoring and Alerts

Use the history data to identify problematic tests:

```kotlin
// Find flaky tests (frequently failing)
fun findFlakyTests(graph: TestGraph, minExecutions: Int = 10): List<TestMethodNode> {
    val executionManager = ExecutionHistoryManager()
    return graph.modules.flatMap { it.testClasses }
        .flatMap { it.testMethods }
        .filter { method ->
            val stats = executionManager.getExecutionStats(method)
            stats.totalExecutions >= minExecutions && 
            stats.successRate < 0.8 && stats.successRate > 0.2
        }
}

// Find tests requiring many remediation attempts
fun findProblematicTests(graph: TestGraph): List<TestMethodNode> {
    val remediationManager = RemediationHistoryManager()
    return graph.modules.flatMap { it.testClasses }
        .flatMap { it.testMethods }
        .filter { method ->
            val stats = remediationManager.getRemediationStats(method)
            stats.totalAttempts >= 3 && stats.successRate < 0.5
        }
}
```

### 5. Performance Optimization

For large projects (1000+ tests):

```kotlin
// Use custom update strategy to minimize rebuilds
val strategy = GraphUpdateStrategy.CompositeStrategy(
    listOf(
        GraphUpdateStrategy.PomModificationStrategy(),
        GraphUpdateStrategy.TimeBasedStrategy(Duration.ofHours(168)) // 1 week
    )
)

// Or never rebuild unless necessary
val strategy = GraphUpdateStrategy.Never
```

## Troubleshooting

### Graph Not Building

**Problem**: Graph is empty or missing tests

**Solution**:
1. Ensure tests are compiled: `mvn test-compile`
2. Check test discovery patterns in `TestGraphBuilder`
3. Verify test files contain "Test" in filename
4. Check JUnit 5 dependencies are present

### History Not Preserved

**Problem**: History lost after graph rebuild

**Solution**:
1. Check that `test-graph.json` exists before rebuild
2. Verify method IDs are consistent (don't change test names)
3. Check for errors in graph merging logic

### Workspace Permission Errors

**Problem**: Cannot create workspace directories

**Solution**:
```bash
# Ensure .browser4tester is writable
chmod -R 755 .browser4tester
```

### Performance Degradation

**Problem**: Graph loading/saving is slow

**Solution**:
1. Check graph size: `du -sh .browser4tester/test-graph.json`
2. If > 10MB, consider reducing history limits
3. Clean up old archives
4. Use faster update strategy

## Future Enhancements

The design supports these future features:

1. **Parallel Execution**: Use DAG to execute independent tests in parallel
2. **Smart Retry**: Use history to predict flaky tests
3. **Trend Analysis**: Identify degrading tests over time
4. **Cost Tracking**: Track Copilot API usage per test
5. **Web Dashboard**: Visualize graph and history
6. **Export**: Generate reports for management
7. **ML Insights**: Predict which tests will fail

## Support

For issues, questions, or contributions:
- GitHub Issues: [Browser4Tester Issues](https://github.com/platonai/Browser4Tester/issues)
- Documentation: See `DESIGN.md` for architecture details
- Tests: See `PersistentTestOrchestratorTest.kt` for examples
