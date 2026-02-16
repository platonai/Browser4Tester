package com.browser4tester.healer.persistence

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Instant

class PersistenceManagerTest {
    
    @Test
    fun `saves and loads graph successfully`(@TempDir tempDir: Path) {
        val manager = PersistenceManager(tempDir)
        
        val graph = TestGraph(
            modules = listOf(
                ModuleNode(
                    id = "module1",
                    name = "test-module",
                    path = "/path/to/module",
                    dependencies = emptyList(),
                    testClasses = listOf(
                        TestClassNode(
                            id = "class1",
                            fullyQualifiedName = "com.example.Test",
                            filePath = "/path/to/Test.kt",
                            testMethods = listOf(
                                TestMethodNode(
                                    id = "method1",
                                    name = "testMethod",
                                    displayName = "Test Method"
                                )
                            )
                        )
                    )
                )
            ),
            lastUpdated = Instant.now()
        )
        
        manager.saveGraph(graph)
        val loaded = manager.loadGraph()
        
        assertNotNull(loaded)
        assertEquals(1, loaded?.modules?.size)
        assertEquals("module1", loaded?.modules?.get(0)?.id)
        assertEquals(1, loaded?.modules?.get(0)?.testClasses?.size)
    }
    
    @Test
    fun `returns null when graph doesn't exist`(@TempDir tempDir: Path) {
        val manager = PersistenceManager(tempDir)
        
        assertFalse(manager.graphExists())
        assertNull(manager.loadGraph())
    }
    
    @Test
    fun `updates method node correctly`(@TempDir tempDir: Path) {
        val manager = PersistenceManager(tempDir)
        
        val method = TestMethodNode(
            id = "method1",
            name = "testMethod",
            displayName = "Test Method"
        )
        
        val graph = TestGraph(
            modules = listOf(
                ModuleNode(
                    id = "module1",
                    name = "test-module",
                    path = "/path",
                    dependencies = emptyList(),
                    testClasses = listOf(
                        TestClassNode(
                            id = "class1",
                            fullyQualifiedName = "com.example.Test",
                            filePath = "/Test.kt",
                            testMethods = listOf(method)
                        )
                    )
                )
            ),
            lastUpdated = Instant.now()
        )
        
        val updatedGraph = manager.updateMethodNode(
            graph = graph,
            moduleId = "module1",
            classId = "class1",
            methodId = "method1"
        ) { node ->
            node.copy(
                lastExecution = ExecutionRecord(
                    timestamp = Instant.now(),
                    result = ExecutionResult.SUCCESS,
                    durationMs = 100
                )
            )
        }
        
        val foundMethod = manager.findMethodNode(
            graph = updatedGraph,
            moduleId = "module1",
            classId = "class1",
            methodId = "method1"
        )
        
        assertNotNull(foundMethod?.lastExecution)
        assertEquals(ExecutionResult.SUCCESS, foundMethod?.lastExecution?.result)
    }
    
    @Test
    fun `finds method node by path`(@TempDir tempDir: Path) {
        val manager = PersistenceManager(tempDir)
        
        val method = TestMethodNode(
            id = "method1",
            name = "testMethod",
            displayName = "Test Method"
        )
        
        val graph = TestGraph(
            modules = listOf(
                ModuleNode(
                    id = "module1",
                    name = "test-module",
                    path = "/path",
                    dependencies = emptyList(),
                    testClasses = listOf(
                        TestClassNode(
                            id = "class1",
                            fullyQualifiedName = "com.example.Test",
                            filePath = "/Test.kt",
                            testMethods = listOf(method)
                        )
                    )
                )
            ),
            lastUpdated = Instant.now()
        )
        
        val found = manager.findMethodNode(graph, "module1", "class1", "method1")
        
        assertNotNull(found)
        assertEquals("method1", found?.id)
        assertEquals("testMethod", found?.name)
    }
    
    @Test
    fun `returns null when method not found`(@TempDir tempDir: Path) {
        val manager = PersistenceManager(tempDir)
        
        val graph = TestGraph(
            modules = emptyList(),
            lastUpdated = Instant.now()
        )
        
        val found = manager.findMethodNode(graph, "module1", "class1", "method1")
        
        assertNull(found)
    }
}
