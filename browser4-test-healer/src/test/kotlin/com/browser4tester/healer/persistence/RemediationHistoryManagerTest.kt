package com.browser4tester.healer.persistence

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class RemediationHistoryManagerTest {
    
    private val manager = RemediationHistoryManager()
    
    @Test
    fun `records remediation successfully`() {
        val method = TestMethodNode(
            id = "test1",
            name = "testMethod",
            displayName = "Test Method"
        )
        
        val updated = manager.recordRemediation(
            methodNode = method,
            result = RemediationResult.SUCCESS,
            diagnosticReport = "Fixed null pointer",
            workspacePath = "/workspace/fix1",
            copilotPrompt = "Fix this test",
            copilotResponse = "Here's the fix",
            changesApplied = listOf("TestClass.kt"),
            durationMs = 5000
        )
        
        assertNotNull(updated.lastRemediation)
        assertEquals(RemediationResult.SUCCESS, updated.lastRemediation?.result)
        assertEquals("Fixed null pointer", updated.lastRemediation?.diagnosticReport)
        assertEquals(1, updated.remediationHistory.size)
    }
    
    @Test
    fun `maintains history limit`() {
        var method = TestMethodNode(
            id = "test1",
            name = "testMethod",
            displayName = "Test Method"
        )
        
        // Add 25 remediations (exceeds max of 20)
        repeat(25) {
            method = manager.recordRemediation(
                methodNode = method,
                result = RemediationResult.SUCCESS,
                diagnosticReport = "Fix $it",
                workspacePath = "/workspace/fix$it",
                copilotPrompt = "Fix",
                copilotResponse = "Fixed",
                changesApplied = emptyList(),
                durationMs = 1000
            )
        }
        
        // Should only keep last 20
        assertEquals(20, method.remediationHistory.size)
    }
    
    @Test
    fun `requires remediation when last execution failed`() {
        val method = TestMethodNode(
            id = "test1",
            name = "testMethod",
            displayName = "Test Method",
            lastExecution = ExecutionRecord(
                timestamp = Instant.now(),
                result = ExecutionResult.FAILURE,
                durationMs = 100,
                errorMessage = "Test failed"
            )
        )
        
        assertTrue(manager.requiresRemediation(method))
    }
    
    @Test
    fun `does not require remediation when last execution passed`() {
        val method = TestMethodNode(
            id = "test1",
            name = "testMethod",
            displayName = "Test Method",
            lastExecution = ExecutionRecord(
                timestamp = Instant.now(),
                result = ExecutionResult.SUCCESS,
                durationMs = 100
            )
        )
        
        assertFalse(manager.requiresRemediation(method))
    }
    
    @Test
    fun `requires remediation when last execution failed after successful remediation`() {
        val remediationTime = Instant.now().minusSeconds(60)
        val executionTime = Instant.now()
        
        val method = TestMethodNode(
            id = "test1",
            name = "testMethod",
            displayName = "Test Method",
            lastExecution = ExecutionRecord(
                timestamp = executionTime,
                result = ExecutionResult.FAILURE,
                durationMs = 100
            ),
            lastRemediation = RemediationRecord(
                timestamp = remediationTime,
                result = RemediationResult.SUCCESS,
                diagnosticReport = "Fixed",
                workspacePath = "/workspace",
                copilotPrompt = "Fix",
                copilotResponse = "Fixed",
                durationMs = 1000
            )
        )
        
        // Execution failed AFTER successful remediation, so needs new remediation
        assertTrue(manager.requiresRemediation(method))
    }
    
    @Test
    fun `calculates remediation stats correctly`() {
        var method = TestMethodNode(
            id = "test1",
            name = "testMethod",
            displayName = "Test Method"
        )
        
        // Add 3 successful, 2 failed
        repeat(3) {
            method = manager.recordRemediation(
                methodNode = method,
                result = RemediationResult.SUCCESS,
                diagnosticReport = "Fixed",
                workspacePath = "/workspace",
                copilotPrompt = "Fix",
                copilotResponse = "Fixed",
                changesApplied = emptyList(),
                durationMs = 1000
            )
        }
        repeat(2) {
            method = manager.recordRemediation(
                methodNode = method,
                result = RemediationResult.FAILURE,
                diagnosticReport = "Failed to fix",
                workspacePath = "/workspace",
                copilotPrompt = "Fix",
                copilotResponse = "Could not fix",
                changesApplied = emptyList(),
                durationMs = 1000
            )
        }
        
        val stats = manager.getRemediationStats(method)
        
        assertEquals(5, stats.totalAttempts)
        assertEquals(3, stats.successCount)
        assertEquals(2, stats.failureCount)
        assertEquals(0.6, stats.successRate, 0.01)
    }
}
