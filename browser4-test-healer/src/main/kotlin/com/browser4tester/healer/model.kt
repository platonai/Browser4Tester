package com.browser4tester.healer

import java.nio.file.Path

data class FailureDetail(
    val method: String,
    val message: String,
    val stacktrace: String,
)

data class ClassExecutionResult(
    val className: String,
    val failures: List<FailureDetail>,
) {
    val passed: Boolean = failures.isEmpty()
}

data class OrchestratorConfig(
    val maxRetryPerClass: Int = 3,
    val allowMainSourceEdits: Boolean = false,
    val testRoot: Path = Path.of("."),
)

data class RepairContext(
    val className: String,
    val testFile: Path,
    val testSource: String,
    val failures: List<FailureDetail>,
)

data class CopilotRepairResult(
    val updatedFileContent: String,
    val rawOutput: String,
)
