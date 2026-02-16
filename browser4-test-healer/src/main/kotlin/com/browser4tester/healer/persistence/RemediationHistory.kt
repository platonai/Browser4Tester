package com.browser4tester.healer.persistence

import java.time.Instant

/**
 * Manages remediation history for test methods
 */
class RemediationHistoryManager {
    private val maxHistoryPerMethod = 20 // Keep last 20 remediation attempts
    
    /**
     * Records a new remediation attempt for a test method
     */
    fun recordRemediation(
        methodNode: TestMethodNode,
        result: RemediationResult,
        diagnosticReport: String,
        workspacePath: String,
        copilotPrompt: String,
        copilotResponse: String,
        changesApplied: List<String>,
        durationMs: Long
    ): TestMethodNode {
        val record = RemediationRecord(
            timestamp = Instant.now(),
            result = result,
            diagnosticReport = diagnosticReport,
            workspacePath = workspacePath,
            copilotPrompt = copilotPrompt,
            copilotResponse = copilotResponse,
            changesApplied = changesApplied,
            durationMs = durationMs
        )
        
        val updatedHistory = (listOf(record) + methodNode.remediationHistory)
            .take(maxHistoryPerMethod)
        
        return methodNode.copy(
            lastRemediation = record,
            remediationHistory = updatedHistory
        )
    }
    
    /**
     * Gets statistics for a test method's remediation history
     */
    fun getRemediationStats(methodNode: TestMethodNode): RemediationStats {
        val history = methodNode.remediationHistory
        if (history.isEmpty()) {
            return RemediationStats(
                totalAttempts = 0,
                successCount = 0,
                failureCount = 0,
                partialCount = 0,
                skippedCount = 0,
                successRate = 0.0,
                averageDurationMs = 0L,
                lastSuccess = null,
                lastFailure = null
            )
        }
        
        val successCount = history.count { it.result == RemediationResult.SUCCESS }
        val failureCount = history.count { it.result == RemediationResult.FAILURE }
        val partialCount = history.count { it.result == RemediationResult.PARTIAL }
        val skippedCount = history.count { it.result == RemediationResult.SKIPPED }
        
        return RemediationStats(
            totalAttempts = history.size,
            successCount = successCount,
            failureCount = failureCount,
            partialCount = partialCount,
            skippedCount = skippedCount,
            successRate = successCount.toDouble() / history.size,
            averageDurationMs = history.map { it.durationMs }.average().toLong(),
            lastSuccess = history.firstOrNull { it.result == RemediationResult.SUCCESS }?.timestamp,
            lastFailure = history.firstOrNull { it.result == RemediationResult.FAILURE }?.timestamp
        )
    }
    
    /**
     * Determines if a test method requires remediation based on its history
     */
    fun requiresRemediation(methodNode: TestMethodNode): Boolean {
        val lastExecution = methodNode.lastExecution ?: return false
        
        // Requires remediation if last execution failed
        if (lastExecution.result == ExecutionResult.FAILURE || lastExecution.result == ExecutionResult.ERROR) {
            // Check if we've already tried remediation recently
            val lastRemediation = methodNode.lastRemediation
            if (lastRemediation != null) {
                // Only retry if last remediation was not successful or was a while ago
                return lastRemediation.result != RemediationResult.SUCCESS ||
                       lastRemediation.timestamp.isBefore(lastExecution.timestamp)
            }
            return true
        }
        
        return false
    }
}

data class RemediationStats(
    val totalAttempts: Int,
    val successCount: Int,
    val failureCount: Int,
    val partialCount: Int,
    val skippedCount: Int,
    val successRate: Double,
    val averageDurationMs: Long,
    val lastSuccess: Instant?,
    val lastFailure: Instant?
)
