# Example Persisted Test Graph

This document shows what the persisted test graph looks like in practice.

## Example test-graph.json

```json
{
  "modules": [
    {
      "id": "browser4-test-healer",
      "name": "browser4-test-healer",
      "path": "/path/to/project/browser4-test-healer",
      "dependencies": [],
      "testClasses": [
        {
          "id": "browser4-test-healer/com.browser4tester.healer.persistence.ExecutionHistoryManagerTest",
          "fullyQualifiedName": "com.browser4tester.healer.persistence.ExecutionHistoryManagerTest",
          "filePath": "/path/to/project/browser4-test-healer/src/test/kotlin/com/browser4tester/healer/persistence/ExecutionHistoryManagerTest.kt",
          "testMethods": [
            {
              "id": "[engine:junit-jupiter]/[class:com.browser4tester.healer.persistence.ExecutionHistoryManagerTest]/[method:records execution successfully()]",
              "name": "records execution successfully()",
              "displayName": "records execution successfully()",
              "lastExecution": {
                "timestamp": "2026-02-16T06:28:30.123Z",
                "result": "SUCCESS",
                "durationMs": 15,
                "errorMessage": null,
                "stackTrace": null,
                "mavenLogPath": null,
                "surefireReportPath": null
              },
              "lastRemediation": null,
              "executionHistory": [
                {
                  "timestamp": "2026-02-16T06:28:30.123Z",
                  "result": "SUCCESS",
                  "durationMs": 15,
                  "errorMessage": null,
                  "stackTrace": null,
                  "mavenLogPath": null,
                  "surefireReportPath": null
                },
                {
                  "timestamp": "2026-02-16T05:15:20.456Z",
                  "result": "SUCCESS",
                  "durationMs": 12,
                  "errorMessage": null,
                  "stackTrace": null,
                  "mavenLogPath": null,
                  "surefireReportPath": null
                }
              ],
              "remediationHistory": []
            },
            {
              "id": "[engine:junit-jupiter]/[class:com.browser4tester.healer.persistence.ExecutionHistoryManagerTest]/[method:calculates statistics correctly()]",
              "name": "calculates statistics correctly()",
              "displayName": "calculates statistics correctly()",
              "lastExecution": {
                "timestamp": "2026-02-16T06:28:30.145Z",
                "result": "FAILURE",
                "durationMs": 8,
                "errorMessage": "Expected 0.75 but was 0.5",
                "stackTrace": "org.opentest4j.AssertionFailedError: Expected 0.75 but was 0.5\n\tat ExecutionHistoryManagerTest.calculates statistics correctly()(ExecutionHistoryManagerTest.kt:45)",
                "mavenLogPath": null,
                "surefireReportPath": null
              },
              "lastRemediation": {
                "timestamp": "2026-02-16T06:29:15.789Z",
                "result": "SUCCESS",
                "diagnosticReport": "AI remediation attempt",
                "workspacePath": ".browser4tester/remediation/ExecutionHistoryManagerTest_calculatesStatisticsCorrectly_2026-02-16T06-29-15",
                "copilotPrompt": "Fix failing test",
                "copilotResponse": "Updated test expectations",
                "changesApplied": [
                  "src/test/kotlin/com/browser4tester/healer/persistence/ExecutionHistoryManagerTest.kt"
                ],
                "durationMs": 15234
              },
              "executionHistory": [
                {
                  "timestamp": "2026-02-16T06:29:18.123Z",
                  "result": "SUCCESS",
                  "durationMs": 9,
                  "errorMessage": null,
                  "stackTrace": null,
                  "mavenLogPath": null,
                  "surefireReportPath": null
                },
                {
                  "timestamp": "2026-02-16T06:28:30.145Z",
                  "result": "FAILURE",
                  "durationMs": 8,
                  "errorMessage": "Expected 0.75 but was 0.5",
                  "stackTrace": "org.opentest4j.AssertionFailedError: Expected 0.75 but was 0.5\n\tat ExecutionHistoryManagerTest.calculates statistics correctly()(ExecutionHistoryManagerTest.kt:45)",
                  "mavenLogPath": null,
                  "surefireReportPath": null
                }
              ],
              "remediationHistory": [
                {
                  "timestamp": "2026-02-16T06:29:15.789Z",
                  "result": "SUCCESS",
                  "diagnosticReport": "AI remediation attempt",
                  "workspacePath": ".browser4tester/remediation/ExecutionHistoryManagerTest_calculatesStatisticsCorrectly_2026-02-16T06-29-15",
                  "copilotPrompt": "Fix failing test",
                  "copilotResponse": "Updated test expectations",
                  "changesApplied": [
                    "src/test/kotlin/com/browser4tester/healer/persistence/ExecutionHistoryManagerTest.kt"
                  ],
                  "durationMs": 15234
                }
              ]
            }
          ],
          "dependencies": []
        }
      ]
    }
  ],
  "lastUpdated": "2026-02-16T06:28:44.567Z",
  "version": "1.0"
}
```

## Example Remediation Workspace

### Directory Structure

```
.browser4tester/remediation/ExecutionHistoryManagerTest_calculatesStatisticsCorrectly_2026-02-16T06-29-15/
├── README.md
├── failure-context.txt
├── copilot-prompt.txt
├── copilot-response.txt
├── diagnostic-report.md
├── applied-changes/
│   └── ExecutionHistoryManagerTest.kt
└── logs/
    └── activity.log
```

### README.md

```markdown
# Remediation Workspace

**Test Class**: `com.browser4tester.healer.persistence.ExecutionHistoryManagerTest`
**Test Method**: `calculatesStatisticsCorrectly`
**Created**: 2026-02-16T06:29:15.789Z

## Purpose
This workspace contains all artifacts related to the automated remediation
of the failing test method.

## Structure
- `README.md` - This file
- `failure-context.txt` - Details of the test failure
- `copilot-prompt.txt` - The prompt sent to GitHub Copilot
- `copilot-response.txt` - The response from GitHub Copilot
- `diagnostic-report.md` - AI-generated diagnostic analysis
- `applied-changes/` - Files that were modified during remediation
- `logs/` - Execution and error logs
```

### failure-context.txt

```
=== Test Failure Context ===

Class: com.browser4tester.healer.persistence.ExecutionHistoryManagerTest
Method: calculatesStatisticsCorrectly

=== Error Message ===
Expected 0.75 but was 0.5

=== Stack Trace ===
org.opentest4j.AssertionFailedError: Expected 0.75 but was 0.5
	at org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
	at ExecutionHistoryManagerTest.calculates statistics correctly()(ExecutionHistoryManagerTest.kt:45)

=== Test Source Code ===
@Test
fun `calculates statistics correctly`() {
    val manager = ExecutionHistoryManager()
    var methodNode = TestMethodNode(
        id = "test1",
        name = "testMethod",
        displayName = "Test Method"
    )
    
    // Record 4 executions: 3 success, 1 failure
    methodNode = manager.recordExecution(methodNode, ExecutionResult.SUCCESS, 100)
    methodNode = manager.recordExecution(methodNode, ExecutionResult.SUCCESS, 100)
    methodNode = manager.recordExecution(methodNode, ExecutionResult.FAILURE, 100)
    methodNode = manager.recordExecution(methodNode, ExecutionResult.SUCCESS, 100)
    
    val stats = manager.getExecutionStats(methodNode)
    assertEquals(4, stats.totalExecutions)
    assertEquals(3, stats.successCount)
    assertEquals(1, stats.failureCount)
    assertEquals(0.75, stats.successRate)  // Expected 3/4 = 0.75 but was 0.5
}
```

### logs/activity.log

```
[2026-02-16T06:29:15.789Z] Starting remediation attempt 1
[2026-02-16T06:29:18.234Z] Copilot invoked successfully
[2026-02-16T06:29:18.456Z] Applied patch to ExecutionHistoryManagerTest.kt
[2026-02-16T06:29:18.789Z] Re-running test
[2026-02-16T06:29:19.123Z] Remediation succeeded
```

## Statistics Queries

You can query the persisted data to get insights:

### Find Flaky Tests

```kotlin
val graph = persistenceManager.loadGraph()!!
val executionManager = ExecutionHistoryManager()

val flakyTests = graph.modules
    .flatMap { it.testClasses }
    .flatMap { it.testMethods }
    .filter { method ->
        val stats = executionManager.getExecutionStats(method)
        stats.totalExecutions >= 10 && 
        stats.successRate in 0.2..0.8  // Flaky: sometimes pass, sometimes fail
    }

flakyTests.forEach { test ->
    val stats = executionManager.getExecutionStats(test)
    println("${test.name}: ${stats.successRate * 100}% success rate " +
            "over ${stats.totalExecutions} runs")
}
```

Output:
```
calculatesStatisticsCorrectly: 50.0% success rate over 12 runs
shouldHandleEdgeCases: 66.7% success rate over 15 runs
```

### Find Tests Requiring Many Remediations

```kotlin
val remediationManager = RemediationHistoryManager()

val problematicTests = graph.modules
    .flatMap { it.testClasses }
    .flatMap { it.testMethods }
    .filter { method ->
        val stats = remediationManager.getRemediationStats(method)
        stats.totalAttempts >= 3  // Required 3+ AI fixes
    }

problematicTests.forEach { test ->
    val stats = remediationManager.getRemediationStats(test)
    println("${test.name}: ${stats.totalAttempts} remediation attempts, " +
            "${stats.successRate * 100}% success rate")
}
```

Output:
```
shouldProcessComplexData: 5 remediation attempts, 40.0% success rate
shouldHandleNullValues: 3 remediation attempts, 66.7% success rate
```

### Calculate Average Remediation Time

```kotlin
val allRemediations = graph.modules
    .flatMap { it.testClasses }
    .flatMap { it.testMethods }
    .flatMap { it.remediationHistory }

val avgDuration = allRemediations
    .map { it.durationMs }
    .average()

println("Average remediation time: ${avgDuration / 1000}s")
```

Output:
```
Average remediation time: 15.2s
```

## Archive Examples

Archives preserve historical snapshots:

```
.browser4tester/archives/
├── test-graph-2026-02-16T06-28-44.json
├── test-graph-2026-02-15T14-20-00.json
└── test-graph-2026-02-14T10-15-30.json
```

You can compare archives to track changes over time:

```kotlin
val current = persistenceManager.loadGraph()!!
val archive = loadArchive("test-graph-2026-02-15T14-20-00.json")

val currentTests = current.modules.sumOf { it.testClasses.sumOf { it.testMethods.size } }
val archiveTests = archive.modules.sumOf { it.testClasses.sumOf { it.testMethods.size } }

println("Test count change: ${currentTests - archiveTests} " +
        "(${currentTests} vs ${archiveTests})")
```

Output:
```
Test count change: +15 (215 vs 200)
```

## Size Estimates

Typical sizes for a project with 1000 test methods:

- **test-graph.json**: ~1.5 MB
  - With execution history: ~1 KB per method
  - With remediation history: ~500 bytes per remediation
  
- **Per workspace**: ~10-50 KB
  - README, logs, context: ~5 KB
  - Applied changes (source code): 5-45 KB depending on file size

- **Total for 100 remediations**: ~5 MB
  - Graph: 1.5 MB
  - Workspaces: 3.5 MB (average 35 KB each)

Storage is relatively modest and scales linearly with test count and remediation frequency.
