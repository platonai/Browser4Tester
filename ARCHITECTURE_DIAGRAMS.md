# Architecture Diagrams

## 1. Data Model Hierarchy

```
┌─────────────────────────────────────────────────────────────────┐
│                          TestGraph                               │
│  - modules: List<ModuleNode>                                    │
│  - lastUpdated: Instant                                         │
│  - version: String                                              │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ contains
                         ▼
         ┌───────────────────────────────────┐
         │        ModuleNode                 │
         │  - id: String                     │
         │  - name: String                   │
         │  - path: String                   │
         │  - dependencies: List<String>     │
         │  - testClasses: List<...>         │
         └───────────────┬───────────────────┘
                         │
                         │ contains
                         ▼
         ┌───────────────────────────────────┐
         │      TestClassNode                │
         │  - id: String                     │
         │  - fullyQualifiedName: String     │
         │  - filePath: String               │
         │  - dependencies: List<String>     │
         │  - testMethods: List<...>         │
         └───────────────┬───────────────────┘
                         │
                         │ contains
                         ▼
         ┌─────────────────────────────────────────────┐
         │           TestMethodNode                     │
         │  - id: String                                │
         │  - name: String                              │
         │  - displayName: String                       │
         │  - lastExecution: ExecutionRecord?           │
         │  - lastRemediation: RemediationRecord?       │
         │  - executionHistory: List<ExecutionRecord>   │
         │  - remediationHistory: List<Remediation...>  │
         └────────┬──────────────────────────┬──────────┘
                  │                          │
      ┌───────────▼──────────┐   ┌──────────▼────────────┐
      │  ExecutionRecord     │   │  RemediationRecord    │
      │  - timestamp         │   │  - timestamp          │
      │  - result            │   │  - result             │
      │  - durationMs        │   │  - diagnosticReport   │
      │  - errorMessage      │   │  - workspacePath      │
      │  - stackTrace        │   │  - copilotPrompt      │
      │  - mavenLogPath      │   │  - copilotResponse    │
      │  - surefireReportPath│   │  - changesApplied     │
      └──────────────────────┘   │  - durationMs         │
                                 └───────────────────────┘
```

## 2. System Components

```
┌────────────────────────────────────────────────────────────┐
│                    Browser4Tester                          │
│                Self-Healing Test Orchestrator              │
└─────────────────────┬──────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┬──────────────┐
        │             │             │              │
        ▼             ▼             ▼              ▼
┌───────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐
│   Graph   │  │Execution │  │Remediation│ │  Workspace   │
│ Management│  │ History  │  │  History  │ │  Management  │
└─────┬─────┘  └────┬─────┘  └────┬──────┘ └──────┬───────┘
      │             │              │                │
      │             │              │                │
┌─────▼──────────────▼──────────────▼────────────────▼──────┐
│              PersistenceManager                             │
│  - saveGraph()                                             │
│  - loadGraph()                                             │
│  - updateMethodNode()                                      │
│  - findMethodNode()                                        │
│  - archiveGraph()                                          │
└────────────────────────┬───────────────────────────────────┘
                         │
                         ▼
         ┌───────────────────────────────┐
         │   JSON Storage                │
         │  .browser4tester/             │
         │  ├── test-graph.json          │
         │  ├── archives/                │
         │  │   └── test-graph-*.json    │
         │  └── remediation/             │
         │      └── {workspaces}/        │
         └───────────────────────────────┘
```

## 3. Execution Flow

```
START
  │
  ▼
┌─────────────────────────┐
│  Load or Build Graph    │
│  (GraphUpdateStrategy)  │
└──────────┬──────────────┘
           │
           ▼
     ┌────────────┐
     │ Graph      │◄───────┐
     │ Exists?    │        │
     └──┬────┬────┘        │
        │    │             │
     No │    │ Yes         │
        │    │             │
        ▼    ▼             │
    ┌───────────┐   ┌──────────────┐
    │   Build   │   │ Should       │
    │   Graph   │   │ Update?      │
    │ (Builder) │   │ (Strategy)   │
    └─────┬─────┘   └──┬────┬──────┘
          │            │    │
          │         No │    │ Yes
          │            │    │
          └────────────┴────┘
                       │
                       ▼
           ┌───────────────────────┐
           │  For Each Module      │
           │  (Dependency Order)   │
           └──────────┬────────────┘
                      │
                      ▼
           ┌───────────────────────┐
           │  For Each Class       │
           └──────────┬────────────┘
                      │
                      ▼
           ┌───────────────────────┐
           │  For Each Method      │
           └──────────┬────────────┘
                      │
                      ▼
           ┌─────────────────────────┐
           │  Execute Test           │
           │  (ClassExecutor)        │
           └──────────┬──────────────┘
                      │
                      ▼
           ┌─────────────────────────┐
           │  Record Execution       │
           │  (ExecutionHistory)     │
           └──────────┬──────────────┘
                      │
                      ▼
                ┌─────────┐
                │ Failed? │
                └──┬───┬──┘
                   │   │
                No │   │ Yes
                   │   │
                   │   ▼
                   │ ┌──────────────────────────┐
                   │ │ Requires Remediation?    │
                   │ │ (RemediationHistory)     │
                   │ └──────────┬───────────────┘
                   │            │
                   │         Yes│
                   │            ▼
                   │   ┌─────────────────────────┐
                   │   │ Create Workspace        │
                   │   │ (RemediationWorkspace)  │
                   │   └──────────┬──────────────┘
                   │              │
                   │              ▼
                   │   ┌─────────────────────────┐
                   │   │ Invoke Copilot          │
                   │   │ (CopilotAgent)          │
                   │   └──────────┬──────────────┘
                   │              │
                   │              ▼
                   │   ┌─────────────────────────┐
                   │   │ Record Remediation      │
                   │   │ (RemediationHistory)    │
                   │   └──────────┬──────────────┘
                   │              │
                   │              ▼
                   │   ┌─────────────────────────┐
                   │   │ Re-execute Test         │
                   │   └──────────┬──────────────┘
                   │              │
                   └──────────────┘
                                  │
                                  ▼
                       ┌──────────────────────┐
                       │  Save Updated Graph  │
                       │  (Persistence)       │
                       └──────────┬───────────┘
                                  │
                                  ▼
                                 END
```

## 4. Workspace Structure

```
.browser4tester/
├── test-graph.json               ← Current graph
├── archives/
│   ├── test-graph-2024-01-01T10-00-00Z.json
│   └── test-graph-2024-01-01T11-00-00Z.json
└── remediation/
    ├── MyTest_testMethod_2024-01-01T10-00-00Z/
    │   ├── README.md             ← Self-documenting
    │   ├── failure-context.txt   ← Error + stack trace
    │   ├── copilot-prompt.txt    ← AI prompt
    │   ├── copilot-response.txt  ← AI response
    │   ├── diagnostic-report.md  ← Analysis
    │   ├── applied-changes/      ← Modified files
    │   │   └── MyTest.kt
    │   └── logs/
    │       └── activity.log      ← Audit trail
    └── AnotherTest_otherMethod_2024-01-01T11-00-00Z/
        └── ... (same structure)
```

## 5. Graph Update Decision Tree

```
                    START
                      │
                      ▼
              ┌───────────────┐
              │ Graph Exists? │
              └───┬───────┬───┘
                  │       │
               No │       │ Yes
                  │       │
                  ▼       ▼
            ┌─────────┐  ┌─────────────────────────┐
            │ BUILD   │  │ Apply Update Strategy   │
            │ GRAPH   │  └──┬──────────────────┬───┘
            └─────────┘     │                  │
                            │                  │
                  ┌─────────▼────────┐ ┌──────▼────────┐
                  │ Time-Based       │ │ File-Based    │
                  │ Strategy         │ │ Strategy      │
                  │ (24h threshold)  │ │ (modified?)   │
                  └──────┬───────────┘ └───────┬───────┘
                         │                     │
                         ▼                     ▼
                  ┌──────────────┐    ┌──────────────┐
                  │ Older than   │    │ pom.xml or   │
                  │ 24 hours?    │    │ .kt modified?│
                  └──┬────┬──────┘    └──┬────┬──────┘
                     │    │              │    │
                  Yes│    │No         Yes│    │No
                     │    │              │    │
                     └────┴──────┬───────┴────┘
                                 │
                          ┌──────▼──────┐
                          │   Any YES?  │
                          └──┬────┬─────┘
                             │    │
                          Yes│    │No
                             │    │
                     ┌───────▼────▼──────┐
                     │ REBUILD    USE    │
                     │ GRAPH    CACHED   │
                     └───────────────────┘
```

## 6. History Management

```
TestMethodNode
     │
     ├─── ExecutionHistory (Sliding Window)
     │    │
     │    ├─── [0] Most Recent (lastExecution)
     │    ├─── [1] Previous
     │    ├─── [2] ...
     │    ├─── ...
     │    └─── [49] Oldest (max 50)
     │
     └─── RemediationHistory (Sliding Window)
          │
          ├─── [0] Most Recent (lastRemediation)
          ├─── [1] Previous
          ├─── [2] ...
          ├─── ...
          └─── [19] Oldest (max 20)

Statistics Calculated:
┌────────────────────────────────────────┐
│ ExecutionStats                         │
│ - totalExecutions                      │
│ - successCount / failureCount          │
│ - successRate                          │
│ - averageDurationMs                    │
│ - lastSuccess / lastFailure timestamps │
└────────────────────────────────────────┘

┌────────────────────────────────────────┐
│ RemediationStats                       │
│ - totalAttempts                        │
│ - successCount / failureCount          │
│ - successRate                          │
│ - averageDurationMs                    │
│ - lastSuccess / lastFailure timestamps │
└────────────────────────────────────────┘
```

## 7. Integration Points

```
┌──────────────────────────────────────────────────────────┐
│                  Existing Code                           │
│                                                          │
│  ┌──────────────────┐  ┌──────────────┐  ┌────────────┐│
│  │ TestOrchestrator │  │ ClassExecutor│  │CopilotAgent││
│  └────────┬─────────┘  └──────┬───────┘  └─────┬──────┘│
│           │                   │                 │       │
└───────────┼───────────────────┼─────────────────┼───────┘
            │                   │                 │
            │ uses              │ uses            │ uses
            ▼                   ▼                 ▼
┌──────────────────────────────────────────────────────────┐
│                   New Components                         │
│                                                          │
│  ┌────────────┐  ┌──────────────┐  ┌─────────────────┐ │
│  │ TestGraph  │  │ Execution    │  │ Remediation     │ │
│  │ (DAG)      │  │ History      │  │ Workspace       │ │
│  └────────────┘  └──────────────┘  └─────────────────┘ │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │           PersistenceManager                     │  │
│  │           (JSON Storage)                         │  │
│  └──────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

---

## Implementation Statistics

```
┌─────────────────────────────────────────┐
│        Component Sizes                  │
├─────────────────────────────────────────┤
│ TestGraphModel      :   86 lines       │
│ ExecutionHistory    :  102 lines       │
│ RemediationHistory  :  136 lines       │
│ PersistenceManager  :  126 lines       │
│ TestGraphBuilder    :  192 lines       │
│ GraphUpdateStrategy :  120 lines       │
│ RemediationWorkspace:  181 lines       │
├─────────────────────────────────────────┤
│ Total Implementation:  943 lines       │
├─────────────────────────────────────────┤
│ Unit Tests          :  599 lines       │
│ Documentation       :  830 lines       │
├─────────────────────────────────────────┤
│ Grand Total         : 2372 lines       │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│          Test Coverage                  │
├─────────────────────────────────────────┤
│ ExecutionHistory    :    4 tests       │
│ RemediationHistory  :    6 tests       │
│ PersistenceManager  :    5 tests       │
│ RemediationWorkspace:    8 tests       │
│ (Existing tests)    :    2 tests       │
├─────────────────────────────────────────┤
│ Total               :   25 tests       │
│ Pass Rate           :  100% ✅         │
└─────────────────────────────────────────┘
```
