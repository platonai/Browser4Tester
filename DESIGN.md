# Persistent Core Data Structure Design

## Overview

This document describes the design and implementation of a persistent core data structure for the Browser4Tester self-healing test orchestrator. The system tracks test execution and AI remediation history at the method level, organized in a dependency graph structure.

## Architecture

### 1. Core Data Model (`persistence/TestGraphModel.kt`)

The system is built around a **Directed Acyclic Graph (DAG)** that represents the complete test structure:

```
TestGraph
├── ModuleNode (Maven module)
│   ├── TestClassNode (Test class)
│   │   ├── TestMethodNode (Test method)
│   │   │   ├── ExecutionRecord (history)
│   │   │   └── RemediationRecord (history)
```

#### TestGraph
- Root structure containing all modules
- Tracks last update timestamp
- Version for schema evolution

#### ModuleNode
- Represents a Maven module
- Contains list of test classes
- Tracks module dependencies (other modules)

#### TestClassNode
- Represents a test class
- Contains list of test methods
- Can track class-level dependencies (optional)

#### TestMethodNode
- Represents individual test method
- **lastExecution**: Most recent execution result
- **lastRemediation**: Most recent AI repair attempt
- **executionHistory**: Last 50 executions (configurable)
- **remediationHistory**: Last 20 remediation attempts (configurable)

#### ExecutionRecord
Captures each test execution:
- Timestamp
- Result (SUCCESS, FAILURE, ERROR, SKIPPED)
- Duration in milliseconds
- Error message and stack trace
- Maven log path
- Surefire report path

#### RemediationRecord
Captures each AI repair attempt:
- Timestamp
- Result (SUCCESS, FAILURE, PARTIAL, SKIPPED)
- Diagnostic report
- Workspace path (dedicated folder for this repair)
- Copilot prompt and response
- List of files modified
- Duration in milliseconds

### 2. History Management

#### ExecutionHistoryManager (`persistence/ExecutionHistory.kt`)
- Records new test executions
- Maintains sliding window of history (default: last 50)
- Provides execution statistics:
  - Total executions
  - Success/failure/error/skipped counts
  - Success rate
  - Average duration
  - Last success/failure timestamps

#### RemediationHistoryManager (`persistence/RemediationHistory.kt`)
- Records new remediation attempts
- Maintains sliding window of history (default: last 20)
- Provides remediation statistics
- Determines if method requires remediation based on:
  - Last execution failed
  - No successful remediation after last failure

### 3. Persistence Layer (`persistence/PersistenceManager.kt`)

**Storage Format**: JSON (human-readable, version-controllable)

**Storage Location**: `.browser4tester/`
- `test-graph.json` - Current graph
- `archives/` - Archived snapshots with timestamps

**Features**:
- Load/Save graph to JSON
- Archive old graphs
- Update specific method nodes efficiently
- Find method nodes by ID path

**Dependencies**: Jackson for JSON serialization
- `jackson-databind`
- `jackson-module-kotlin` (Kotlin data class support)
- `jackson-datatype-jsr310` (Java 8 time types)

### 4. Graph Building (`graph/TestGraphBuilder.kt`)

Discovers and builds the dependency graph:

**Discovery Process**:
1. Find all `pom.xml` files in project (identifies modules)
2. For each module:
   - Find test source root (`src/test/kotlin`)
   - Discover test files (files containing "Test")
   - Use JUnit Platform Launcher to discover test methods
3. Parse Maven dependencies (simplified in MVP)

**Result**: Complete graph of all testable code

### 5. Graph Update Strategy (`graph/GraphUpdateStrategy.kt`)

Determines when to refresh the dependency graph:

**Strategies**:
- **Always**: Refresh on every execution
- **Never**: Only build if doesn't exist
- **TimeBasedStrategy**: Refresh if older than N hours (default: 24h)
- **FileModificationStrategy**: Refresh if any test files modified
- **PomModificationStrategy**: Refresh if any pom.xml modified
- **CompositeStrategy**: Combines multiple strategies with OR logic

**Default Strategy**: 
- PomModification OR TimeBasedStrategy(24h)
- Balances freshness with build performance

### 6. Workspace Management (`workspace/RemediationWorkspace.kt`)

Creates dedicated folders for each remediation task:

**Workspace Structure**:
```
.browser4tester/remediation/{ClassName}_{MethodName}_{Timestamp}/
├── README.md                    # Workspace documentation
├── failure-context.txt          # Test failure details
├── copilot-prompt.txt          # AI prompt
├── copilot-response.txt        # AI response
├── diagnostic-report.md        # AI diagnostic analysis
├── applied-changes/            # Modified files
│   └── TestClass.kt
└── logs/
    └── activity.log            # Remediation activity log
```

**Benefits**:
- Complete audit trail for each repair
- Easy to review AI decisions
- Can analyze patterns across repairs
- Workspace is self-documenting

## Execution Logic

### Graph Update Flow

```
1. Check if graph exists
2. Apply GraphUpdateStrategy
   ├─> Should update? 
   │   ├─> Yes: Build new graph (TestGraphBuilder)
   │   └─> No: Load existing graph
3. Save/Archive graph
```

### Test Execution Flow

```
1. Load graph
2. For each module (in dependency order):
   3. For each test class:
      4. For each test method:
         5. Execute test (ClassExecutor)
         6. Record execution (ExecutionHistoryManager)
         7. If failed:
            8. Check if needs remediation (RemediationHistoryManager)
            9. If yes:
               10. Create workspace (RemediationWorkspace)
               11. Invoke Copilot (CopilotAgent)
               12. Record remediation (RemediationHistoryManager)
               13. Re-execute test
12. Save updated graph
```

### AI Remediation Flow

```
1. Identify first failing method (RemediationHistoryManager.requiresRemediation)
2. Create dedicated workspace (RemediationWorkspace)
3. Save failure context:
   - Error message
   - Stack trace
   - Test source code
4. Generate Copilot prompt
5. Invoke GitHub Copilot CLI
6. Save Copilot response
7. Apply changes (PatchApplier)
8. Record remediation attempt
9. Re-execute test
10. Update graph with results
```

## Design Decisions

### Why DAG?
- Natural representation of module/class/method hierarchy
- Supports dependency-ordered execution
- Prevents circular dependencies
- Enables parallel execution in future (within DAG constraints)

### Why JSON?
- Human-readable (can inspect with text editor)
- Version-controllable (can track in git)
- Language-agnostic (can be consumed by other tools)
- Good ecosystem support (Jackson)

### Why Method-Level Tracking?
- Test failures happen at method level
- Remediation targets specific failing methods
- Enables fine-grained statistics
- Allows targeted re-execution

### Why Separate Workspaces?
- Complete audit trail
- Easy debugging ("what did AI do?")
- Pattern analysis across repairs
- Regulatory compliance (in some contexts)
- No pollution of main codebase

### Why Sliding Window History?
- Bounded memory/storage
- Keep recent, relevant history
- Prevent unbounded growth
- Configurable limits

## Integration with Existing Code

### Enhanced TestOrchestrator

The existing `TestOrchestrator` can be enhanced to:
1. Load/build graph before execution
2. Record execution history for each test
3. Use graph to determine execution order
4. Save updated graph after execution

### Enhanced ClassExecutor

Can be enhanced to:
1. Return more detailed execution results
2. Include timing information
3. Capture full error context

### Enhanced CopilotAgent

Can be enhanced to:
1. Create workspace before repair
2. Save all artifacts to workspace
3. Return workspace path in result

## Performance Considerations

### Graph Building
- **Cold start**: Full discovery (~5-30s for large projects)
- **Cached**: JSON load (~100-500ms)
- **Update strategy**: Minimizes unnecessary rebuilds

### Storage
- **Graph size**: ~1KB per test method (with history)
- **Typical project**: 1000 tests = ~1MB JSON
- **Archives**: Automatic cleanup could be added

### Memory
- **Graph in memory**: Modest (< 10MB for large projects)
- **History windows**: Bounded by configuration

## Security & Privacy

### Sensitive Data
- Stack traces may contain paths/data
- Copilot prompts may contain test code
- Responses may contain generated code

### Mitigations
- Store in `.browser4tester/` (can be gitignored)
- Workspace isolation
- Configurable history retention

## Future Enhancements

1. **Parallel Execution**: Use DAG to execute independent tests in parallel
2. **Smart Retry**: Use history to predict flaky tests
3. **Trend Analysis**: Identify degrading tests
4. **Cost Tracking**: Track Copilot API usage per test
5. **Web Dashboard**: Visualize graph and history
6. **Export**: Generate reports for management
7. **Integration**: Slack/Teams notifications
8. **ML Insights**: Predict which tests will fail

## Testing Strategy

### Unit Tests
- Each data model class
- Persistence layer (mock filesystem)
- History managers
- Graph builder (mock project structure)

### Integration Tests
- Full graph build on sample project
- Persistence round-trip
- Workspace creation

### Performance Tests
- Large graph (1000+ tests)
- Multiple graph updates
- History growth over time

## Migration Path

For existing Browser4Tester installations:

1. **Version 1.0**: Current implementation (no persistence)
2. **Version 1.1**: Add persistence (backward compatible)
   - Graph builds on first run
   - Existing orchestrator still works
3. **Version 2.0**: Full integration
   - Orchestrator uses graph
   - History-based decisions

## Summary

This design provides:

✅ **Complete test structure** - DAG of modules/classes/methods  
✅ **Execution tracking** - Method-level history with stats  
✅ **Remediation tracking** - AI repair history with workspaces  
✅ **Persistent storage** - JSON-based, human-readable  
✅ **Smart updates** - Configurable refresh strategies  
✅ **Audit trail** - Dedicated workspaces per repair  
✅ **Scalability** - Bounded history, efficient storage  
✅ **Extensibility** - Easy to add new features

The design balances:
- **Completeness** vs **Simplicity**
- **Performance** vs **Freshness**
- **Detail** vs **Storage**
- **Flexibility** vs **Opinionation**

## File Structure

```
browser4-test-healer/src/main/kotlin/com/browser4tester/healer/
├── model.kt                          # Original models
├── TestOrchestrator.kt              # Original orchestrator
├── ClassExecutor.kt                 # Original executor
├── CopilotAgent.kt                  # Original AI agent
├── GitSnapshotManager.kt            # Original git manager
├── PatchApplier.kt                  # Original patch applier
├── persistence/
│   ├── TestGraphModel.kt           # DAG data structures
│   ├── ExecutionHistory.kt         # Execution tracking
│   ├── RemediationHistory.kt       # Remediation tracking
│   └── PersistenceManager.kt       # JSON storage
├── graph/
│   ├── TestGraphBuilder.kt         # Graph discovery
│   └── GraphUpdateStrategy.kt      # Update policies
└── workspace/
    └── RemediationWorkspace.kt     # Workspace management
```

## Dependencies Added

```xml
<!-- Jackson for JSON serialization -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.16.1</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.module</groupId>
    <artifactId>jackson-module-kotlin</artifactId>
    <version>2.16.1</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
    <version>2.16.1</version>
</dependency>
```

---

**Status**: ✅ Design Complete & Implemented  
**Build Status**: ✅ Compiles Successfully  
**Next Steps**: Integration testing and orchestrator enhancement
