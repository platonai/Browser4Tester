package com.browser4tester.healer.persistence

import java.time.Instant

/**
 * Directed Acyclic Graph (DAG) representing test structure and dependencies
 */
data class TestGraph(
    val modules: List<ModuleNode>,
    val lastUpdated: Instant,
    val version: String = "1.0"
)

/**
 * Represents a Maven module in the dependency graph
 */
data class ModuleNode(
    val id: String,
    val name: String,
    val path: String,
    val dependencies: List<String>, // Module IDs this module depends on
    val testClasses: List<TestClassNode>
)

/**
 * Represents a test class within a module
 */
data class TestClassNode(
    val id: String,
    val fullyQualifiedName: String,
    val filePath: String,
    val testMethods: List<TestMethodNode>,
    val dependencies: List<String> = emptyList() // Other test class IDs this class depends on
)

/**
 * Represents a test method with its metadata
 */
data class TestMethodNode(
    val id: String,
    val name: String,
    val displayName: String,
    val lastExecution: ExecutionRecord? = null,
    val lastRemediation: RemediationRecord? = null,
    val executionHistory: List<ExecutionRecord> = emptyList(),
    val remediationHistory: List<RemediationRecord> = emptyList()
)

/**
 * Records a single test execution attempt
 */
data class ExecutionRecord(
    val timestamp: Instant,
    val result: ExecutionResult,
    val durationMs: Long,
    val errorMessage: String? = null,
    val stackTrace: String? = null,
    val mavenLogPath: String? = null,
    val surefireReportPath: String? = null
)

enum class ExecutionResult {
    SUCCESS,
    FAILURE,
    ERROR,
    SKIPPED
}

/**
 * Records a single AI remediation attempt
 */
data class RemediationRecord(
    val timestamp: Instant,
    val result: RemediationResult,
    val diagnosticReport: String,
    val workspacePath: String,
    val copilotPrompt: String,
    val copilotResponse: String,
    val changesApplied: List<String> = emptyList(), // File paths that were modified
    val durationMs: Long
)

enum class RemediationResult {
    SUCCESS,
    FAILURE,
    PARTIAL,
    SKIPPED
}
