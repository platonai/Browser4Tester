# Browser4Tester

This repository now contains an MVP module `browser4-test-healer` implementing a class-level self-healing test orchestrator for Kotlin/JUnit 5 projects.

## What it does

- Executes test classes one-by-one via JUnit Platform Launcher API.
- On failure, collects method-level failure metadata (message + stacktrace).
- Calls `gh copilot suggest` with a repair prompt scoped to the failing test class.
- Applies full-file patch output, validates test integrity heuristics, stages changes, and reruns same class.
- Retries per class up to `maxRetryPerClass`, then rolls back snapshot commit if still failing.

## Run

```bash
mvn -pl browser4-test-healer test
mvn -pl browser4-test-healer package
java -jar browser4-test-healer/target/browser4-test-healer-0.1.0-SNAPSHOT.jar com.example.MyTest
```

> Note: the current CLI adapter expects `gh copilot` to return the **full updated file** body.
