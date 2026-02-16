package com.browser4tester.healer.persistence

import java.time.Instant

/**
 * Manages execution history for test methods
 */
class ExecutionHistoryManager {
    private val maxHistoryPerMethod = 50 // Keep last 50 executions
    
    /**
     * Records a new execution for a test method
     */
    fun recordExecution(
        methodNode: TestMethodNode,
        result: ExecutionResult,
        durationMs: Long,
        errorMessage: String? = null,
        stackTrace: String? = null,
        mavenLogPath: String? = null,
        surefireReportPath: String? = null
    ): TestMethodNode {
        val record = ExecutionRecord(
            timestamp = Instant.now(),
            result = result,
            durationMs = durationMs,
            errorMessage = errorMessage,
            stackTrace = stackTrace,
            mavenLogPath = mavenLogPath,
            surefireReportPath = surefireReportPath
        )
        
        val updatedHistory = (listOf(record) + methodNode.executionHistory)
            .take(maxHistoryPerMethod)
        
        return methodNode.copy(
            lastExecution = record,
            executionHistory = updatedHistory
        )
    }
    
    /**
     * Gets statistics for a test method's execution history
     */
    fun getExecutionStats(methodNode: TestMethodNode): ExecutionStats {
        val history = methodNode.executionHistory
        if (history.isEmpty()) {
            return ExecutionStats(
                totalExecutions = 0,
                successCount = 0,
                failureCount = 0,
                errorCount = 0,
                skippedCount = 0,
                successRate = 0.0,
                averageDurationMs = 0L,
                lastSuccess = null,
                lastFailure = null
            )
        }
        
        val successCount = history.count { it.result == ExecutionResult.SUCCESS }
        val failureCount = history.count { it.result == ExecutionResult.FAILURE }
        val errorCount = history.count { it.result == ExecutionResult.ERROR }
        val skippedCount = history.count { it.result == ExecutionResult.SKIPPED }
        
        return ExecutionStats(
            totalExecutions = history.size,
            successCount = successCount,
            failureCount = failureCount,
            errorCount = errorCount,
            skippedCount = skippedCount,
            successRate = successCount.toDouble() / history.size,
            averageDurationMs = history.map { it.durationMs }.average().toLong(),
            lastSuccess = history.firstOrNull { it.result == ExecutionResult.SUCCESS }?.timestamp,
            lastFailure = history.firstOrNull { it.result == ExecutionResult.FAILURE }?.timestamp
        )
    }
}

data class ExecutionStats(
    val totalExecutions: Int,
    val successCount: Int,
    val failureCount: Int,
    val errorCount: Int,
    val skippedCount: Int,
    val successRate: Double,
    val averageDurationMs: Long,
    val lastSuccess: Instant?,
    val lastFailure: Instant?
)
