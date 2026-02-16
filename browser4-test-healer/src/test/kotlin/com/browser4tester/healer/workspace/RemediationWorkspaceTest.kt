package com.browser4tester.healer.workspace

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

class RemediationWorkspaceTest {
    
    @Test
    fun `creates workspace with documentation`(@TempDir tempDir: Path) {
        val workspace = RemediationWorkspace(tempDir)
        
        val context = workspace.createWorkspace(
            className = "com.example.MyTest",
            methodName = "testSomething"
        )
        
        assertTrue(Files.exists(context.path))
        assertTrue(Files.exists(context.path.resolve("README.md")))
        
        val readme = Files.readString(context.path.resolve("README.md"))
        assertTrue(readme.contains("com.example.MyTest"))
        assertTrue(readme.contains("testSomething"))
    }
    
    @Test
    fun `saves failure context`(@TempDir tempDir: Path) {
        val workspace = RemediationWorkspace(tempDir)
        val context = workspace.createWorkspace("Test", "method")
        
        workspace.saveFailureContext(
            context = context,
            errorMessage = "Assertion failed",
            stackTrace = "at Test.method(Test.kt:10)",
            testSource = "fun testMethod() { ... }"
        )
        
        val contextFile = context.path.resolve("failure-context.txt")
        assertTrue(Files.exists(contextFile))
        
        val content = Files.readString(contextFile)
        assertTrue(content.contains("Assertion failed"))
        assertTrue(content.contains("at Test.method"))
    }
    
    @Test
    fun `saves copilot prompt and response`(@TempDir tempDir: Path) {
        val workspace = RemediationWorkspace(tempDir)
        val context = workspace.createWorkspace("Test", "method")
        
        workspace.saveCopilotPrompt(context, "Fix this test")
        workspace.saveCopilotResponse(context, "Here's the fix")
        
        assertTrue(Files.exists(context.path.resolve("copilot-prompt.txt")))
        assertTrue(Files.exists(context.path.resolve("copilot-response.txt")))
        
        assertEquals("Fix this test", Files.readString(context.path.resolve("copilot-prompt.txt")))
        assertEquals("Here's the fix", Files.readString(context.path.resolve("copilot-response.txt")))
    }
    
    @Test
    fun `saves diagnostic report`(@TempDir tempDir: Path) {
        val workspace = RemediationWorkspace(tempDir)
        val context = workspace.createWorkspace("Test", "method")
        
        workspace.saveDiagnosticReport(context, "# Analysis\n\nThe test failed because...")
        
        val reportFile = context.path.resolve("diagnostic-report.md")
        assertTrue(Files.exists(reportFile))
        assertTrue(Files.readString(reportFile).contains("The test failed because"))
    }
    
    @Test
    fun `saves modified files`(@TempDir tempDir: Path) {
        val workspace = RemediationWorkspace(tempDir)
        val context = workspace.createWorkspace("Test", "method")
        
        workspace.saveModifiedFile(context, "/path/to/Test.kt", "fixed code")
        
        val changesDir = context.path.resolve("applied-changes")
        assertTrue(Files.exists(changesDir))
        assertTrue(Files.exists(changesDir.resolve("Test.kt")))
        assertEquals("fixed code", Files.readString(changesDir.resolve("Test.kt")))
    }
    
    @Test
    fun `logs activity`(@TempDir tempDir: Path) {
        val workspace = RemediationWorkspace(tempDir)
        val context = workspace.createWorkspace("Test", "method")
        
        workspace.logActivity(context, "Started remediation")
        workspace.logActivity(context, "Called Copilot")
        
        val logFile = context.path.resolve("logs/activity.log")
        assertTrue(Files.exists(logFile))
        
        val log = Files.readString(logFile)
        assertTrue(log.contains("Started remediation"))
        assertTrue(log.contains("Called Copilot"))
    }
    
    @Test
    fun `lists workspaces`(@TempDir tempDir: Path) {
        val workspace = RemediationWorkspace(tempDir)
        
        workspace.createWorkspace("Test1", "method1")
        workspace.createWorkspace("Test2", "method2")
        
        val workspaces = workspace.listWorkspaces()
        assertEquals(2, workspaces.size)
    }
    
    @Test
    fun `generates unique workspace names`(@TempDir tempDir: Path) {
        val workspace = RemediationWorkspace(tempDir)
        
        val context1 = workspace.createWorkspace("Test", "method", Instant.parse("2024-01-01T10:00:00Z"))
        val context2 = workspace.createWorkspace("Test", "method", Instant.parse("2024-01-01T11:00:00Z"))
        
        assertNotEquals(context1.path, context2.path)
    }
}
