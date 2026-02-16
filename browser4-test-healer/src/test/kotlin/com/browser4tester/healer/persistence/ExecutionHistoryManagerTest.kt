package com.browser4tester.healer.persistence

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class ExecutionHistoryManagerTest {
    
    private val manager = ExecutionHistoryManager()
    
    @Test
    fun `records execution successfully`() {
        val method = TestMethodNode(
            id = "test1",
            name = "testMethod",
            displayName = "Test Method"
        )
        
        val updated = manager.recordExecution(
            methodNode = method,
            result = ExecutionResult.SUCCESS,
            durationMs = 100
        )
        
        assertNotNull(updated.lastExecution)
        assertEquals(ExecutionResult.SUCCESS, updated.lastExecution?.result)
        assertEquals(100, updated.lastExecution?.durationMs)
        assertEquals(1, updated.executionHistory.size)
    }
    
    @Test
    fun `maintains history limit`() {
        var method = TestMethodNode(
            id = "test1",
            name = "testMethod",
            displayName = "Test Method"
        )
        
        // Add 60 executions (exceeds max of 50)
        repeat(60) {
            method = manager.recordExecution(
                methodNode = method,
                result = ExecutionResult.SUCCESS,
                durationMs = 100
            )
        }
        
        // Should only keep last 50
        assertEquals(50, method.executionHistory.size)
        assertNotNull(method.lastExecution)
    }
    
    @Test
    fun `calculates execution stats correctly`() {
        var method = TestMethodNode(
            id = "test1",
            name = "testMethod",
            displayName = "Test Method"
        )
        
        // Add 5 successful, 3 failed, 2 error
        repeat(5) {
            method = manager.recordExecution(
                methodNode = method,
                result = ExecutionResult.SUCCESS,
                durationMs = 100
            )
        }
        repeat(3) {
            method = manager.recordExecution(
                methodNode = method,
                result = ExecutionResult.FAILURE,
                durationMs = 200
            )
        }
        repeat(2) {
            method = manager.recordExecution(
                methodNode = method,
                result = ExecutionResult.ERROR,
                durationMs = 150
            )
        }
        
        val stats = manager.getExecutionStats(method)
        
        assertEquals(10, stats.totalExecutions)
        assertEquals(5, stats.successCount)
        assertEquals(3, stats.failureCount)
        assertEquals(2, stats.errorCount)
        assertEquals(0.5, stats.successRate, 0.01)
    }
    
    @Test
    fun `handles empty history`() {
        val method = TestMethodNode(
            id = "test1",
            name = "testMethod",
            displayName = "Test Method"
        )
        
        val stats = manager.getExecutionStats(method)
        
        assertEquals(0, stats.totalExecutions)
        assertEquals(0.0, stats.successRate)
        assertNull(stats.lastSuccess)
        assertNull(stats.lastFailure)
    }
}
