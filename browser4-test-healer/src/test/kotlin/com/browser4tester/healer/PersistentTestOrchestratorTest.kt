package com.browser4tester.healer

import com.browser4tester.healer.persistence.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * Integration test for PersistentTestOrchestrator
 * 
 * Tests the complete workflow:
 * 1. Graph building and persistence
 * 2. Test execution tracking
 * 3. Remediation history recording
 * 4. Graph updating and merging
 */
class PersistentTestOrchestratorTest {
    
    @Test
    fun `should persist graph on first run`(@TempDir tempDir: Path) {
        // Setup
        val persistenceManager = PersistenceManager(tempDir.resolve(".browser4tester"))
        
        // Create a simple test graph
        val testGraph = TestGraph(
            modules = listOf(
                ModuleNode(
                    id = "test-module",
                    name = "test-module",
                    path = tempDir.toString(),
                    dependencies = emptyList(),
                    testClasses = listOf(
                        TestClassNode(
                            id = "test-class",
                            fullyQualifiedName = "com.example.TestClass",
                            filePath = "$tempDir/src/test/kotlin/TestClass.kt",
                            testMethods = listOf(
                                TestMethodNode(
                                    id = "test-method",
                                    name = "testMethod",
                                    displayName = "Test Method"
                                )
                            )
                        )
                    )
                )
            ),
            lastUpdated = java.time.Instant.now()
        )
        
        // Act
        persistenceManager.saveGraph(testGraph)
        
        // Assert
        assertTrue(persistenceManager.graphExists())
        val loadedGraph = persistenceManager.loadGraph()
        assertNotNull(loadedGraph)
        assertEquals(1, loadedGraph!!.modules.size)
        assertEquals("test-module", loadedGraph.modules[0].id)
    }
    
    @Test
    fun `should record execution history`() {
        // Setup
        val manager = ExecutionHistoryManager()
        val methodNode = TestMethodNode(
            id = "test-1",
            name = "testMethod",
            displayName = "Test Method"
        )
        
        // Act - Record successful execution
        val updated1 = manager.recordExecution(
            methodNode = methodNode,
            result = ExecutionResult.SUCCESS,
            durationMs = 100
        )
        
        // Act - Record failed execution
        val updated2 = manager.recordExecution(
            methodNode = updated1,
            result = ExecutionResult.FAILURE,
            durationMs = 150,
            errorMessage = "Test failed",
            stackTrace = "at TestClass.testMethod()"
        )
        
        // Assert
        assertEquals(2, updated2.executionHistory.size)
        assertEquals(ExecutionResult.FAILURE, updated2.lastExecution?.result)
        assertEquals("Test failed", updated2.lastExecution?.errorMessage)
        
        val stats = manager.getExecutionStats(updated2)
        assertEquals(2, stats.totalExecutions)
        assertEquals(1, stats.successCount)
        assertEquals(1, stats.failureCount)
        assertEquals(0.5, stats.successRate)
    }
    
    @Test
    fun `should record remediation history`(@TempDir tempDir: Path) {
        // Setup
        val manager = RemediationHistoryManager()
        val methodNode = TestMethodNode(
            id = "test-1",
            name = "testMethod",
            displayName = "Test Method",
            lastExecution = ExecutionRecord(
                timestamp = java.time.Instant.now(),
                result = ExecutionResult.FAILURE,
                durationMs = 100,
                errorMessage = "Test failed"
            )
        )
        
        // Act - Record remediation attempt
        val updated = manager.recordRemediation(
            methodNode = methodNode,
            result = RemediationResult.SUCCESS,
            diagnosticReport = "Fixed the test",
            workspacePath = tempDir.resolve("workspace").toString(),
            copilotPrompt = "Fix this test",
            copilotResponse = "Updated code",
            changesApplied = listOf("TestClass.kt"),
            durationMs = 5000
        )
        
        // Assert
        assertEquals(1, updated.remediationHistory.size)
        assertEquals(RemediationResult.SUCCESS, updated.lastRemediation?.result)
        assertEquals("Fixed the test", updated.lastRemediation?.diagnosticReport)
        
        val stats = manager.getRemediationStats(updated)
        assertEquals(1, stats.totalAttempts)
        assertEquals(1, stats.successCount)
        assertEquals(1.0, stats.successRate)
    }
    
    @Test
    fun `should detect when remediation is required`() {
        // Setup
        val manager = RemediationHistoryManager()
        
        // Test 1: Method with no execution history - no remediation needed
        val method1 = TestMethodNode(
            id = "test-1",
            name = "testMethod1",
            displayName = "Test Method 1"
        )
        assertEquals(false, manager.requiresRemediation(method1))
        
        // Test 2: Method with successful execution - no remediation needed
        val method2 = TestMethodNode(
            id = "test-2",
            name = "testMethod2",
            displayName = "Test Method 2",
            lastExecution = ExecutionRecord(
                timestamp = java.time.Instant.now(),
                result = ExecutionResult.SUCCESS,
                durationMs = 100
            )
        )
        assertEquals(false, manager.requiresRemediation(method2))
        
        // Test 3: Method with failed execution - remediation needed
        val method3 = TestMethodNode(
            id = "test-3",
            name = "testMethod3",
            displayName = "Test Method 3",
            lastExecution = ExecutionRecord(
                timestamp = java.time.Instant.now(),
                result = ExecutionResult.FAILURE,
                durationMs = 100,
                errorMessage = "Test failed"
            )
        )
        assertEquals(true, manager.requiresRemediation(method3))
        
        // Test 4: Method with failed execution and successful remediation - no remediation needed
        val method4 = method3.copy(
            lastRemediation = RemediationRecord(
                timestamp = java.time.Instant.now().plusSeconds(10),
                result = RemediationResult.SUCCESS,
                diagnosticReport = "Fixed",
                workspacePath = "/tmp/workspace",
                copilotPrompt = "Fix",
                copilotResponse = "Fixed",
                durationMs = 5000
            )
        )
        assertEquals(false, manager.requiresRemediation(method4))
    }
    
    @Test
    fun `should maintain bounded history windows`() {
        // Setup
        val execManager = ExecutionHistoryManager()
        var methodNode = TestMethodNode(
            id = "test-1",
            name = "testMethod",
            displayName = "Test Method"
        )
        
        // Act - Record 100 executions (max is 50)
        repeat(100) {
            methodNode = execManager.recordExecution(
                methodNode = methodNode,
                result = ExecutionResult.SUCCESS,
                durationMs = 100
            )
        }
        
        // Assert - Should keep only last 50
        assertEquals(50, methodNode.executionHistory.size)
        
        // The most recent execution should be the last one recorded
        assertNotNull(methodNode.lastExecution)
    }
    
    @Test
    fun `should update method node in graph`(@TempDir tempDir: Path) {
        // Setup
        val persistenceManager = PersistenceManager(tempDir.resolve(".browser4tester"))
        val graph = TestGraph(
            modules = listOf(
                ModuleNode(
                    id = "module-1",
                    name = "module-1",
                    path = tempDir.toString(),
                    dependencies = emptyList(),
                    testClasses = listOf(
                        TestClassNode(
                            id = "class-1",
                            fullyQualifiedName = "com.example.TestClass",
                            filePath = "$tempDir/TestClass.kt",
                            testMethods = listOf(
                                TestMethodNode(
                                    id = "method-1",
                                    name = "testMethod",
                                    displayName = "Test Method"
                                )
                            )
                        )
                    )
                )
            ),
            lastUpdated = java.time.Instant.now()
        )
        
        // Act - Update method node
        val updatedGraph = persistenceManager.updateMethodNode(
            graph = graph,
            moduleId = "module-1",
            classId = "class-1",
            methodId = "method-1"
        ) { method ->
            method.copy(
                lastExecution = ExecutionRecord(
                    timestamp = java.time.Instant.now(),
                    result = ExecutionResult.SUCCESS,
                    durationMs = 100
                )
            )
        }
        
        // Assert
        val updatedMethod = persistenceManager.findMethodNode(
            graph = updatedGraph,
            moduleId = "module-1",
            classId = "class-1",
            methodId = "method-1"
        )
        assertNotNull(updatedMethod)
        assertNotNull(updatedMethod!!.lastExecution)
        assertEquals(ExecutionResult.SUCCESS, updatedMethod.lastExecution?.result)
    }
}
