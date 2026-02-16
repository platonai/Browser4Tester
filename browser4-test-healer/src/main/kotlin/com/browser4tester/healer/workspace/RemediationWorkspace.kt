package com.browser4tester.healer.workspace

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Manages dedicated workspaces for each remediation task
 */
class RemediationWorkspace(
    private val workspaceRoot: Path = Path.of(".browser4tester/remediation")
) {
    
    /**
     * Creates a new workspace for a remediation task
     */
    fun createWorkspace(
        className: String,
        methodName: String,
        timestamp: Instant = Instant.now()
    ): WorkspaceContext {
        val workspaceName = generateWorkspaceName(className, methodName, timestamp)
        val workspacePath = workspaceRoot.resolve(workspaceName)
        
        Files.createDirectories(workspacePath)
        
        val context = WorkspaceContext(
            path = workspacePath,
            className = className,
            methodName = methodName,
            timestamp = timestamp
        )
        
        // Create initial documentation
        createWorkspaceDocumentation(context)
        
        return context
    }
    
    /**
     * Generates a unique workspace name
     */
    private fun generateWorkspaceName(
        className: String,
        methodName: String,
        timestamp: Instant
    ): String {
        val simpleName = className.substringAfterLast('.')
        val timestampStr = DateTimeFormatter.ISO_INSTANT
            .format(timestamp)
            .replace(":", "-")
            .replace(".", "-")
        return "${simpleName}_${methodName}_$timestampStr"
    }
    
    /**
     * Creates documentation files in the workspace
     */
    private fun createWorkspaceDocumentation(context: WorkspaceContext) {
        val readmePath = context.path.resolve("README.md")
        val readme = buildString {
            appendLine("# Remediation Workspace")
            appendLine()
            appendLine("**Test Class**: `${context.className}`")
            appendLine("**Test Method**: `${context.methodName}`")
            appendLine("**Created**: ${context.timestamp}")
            appendLine()
            appendLine("## Purpose")
            appendLine("This workspace contains all artifacts related to the automated remediation")
            appendLine("of the failing test method.")
            appendLine()
            appendLine("## Structure")
            appendLine("- `README.md` - This file")
            appendLine("- `failure-context.txt` - Details of the test failure")
            appendLine("- `copilot-prompt.txt` - The prompt sent to GitHub Copilot")
            appendLine("- `copilot-response.txt` - The response from GitHub Copilot")
            appendLine("- `diagnostic-report.md` - AI-generated diagnostic analysis")
            appendLine("- `applied-changes/` - Files that were modified during remediation")
            appendLine("- `logs/` - Execution and error logs")
        }
        
        Files.writeString(readmePath, readme)
    }
    
    /**
     * Saves failure context to the workspace
     */
    fun saveFailureContext(
        context: WorkspaceContext,
        errorMessage: String,
        stackTrace: String,
        testSource: String
    ) {
        val contextPath = context.path.resolve("failure-context.txt")
        val content = buildString {
            appendLine("=== Test Failure Context ===")
            appendLine()
            appendLine("Class: ${context.className}")
            appendLine("Method: ${context.methodName}")
            appendLine()
            appendLine("=== Error Message ===")
            appendLine(errorMessage)
            appendLine()
            appendLine("=== Stack Trace ===")
            appendLine(stackTrace)
            appendLine()
            appendLine("=== Test Source Code ===")
            appendLine(testSource)
        }
        
        Files.writeString(contextPath, content)
    }
    
    /**
     * Saves the Copilot prompt
     */
    fun saveCopilotPrompt(context: WorkspaceContext, prompt: String) {
        val promptPath = context.path.resolve("copilot-prompt.txt")
        Files.writeString(promptPath, prompt)
    }
    
    /**
     * Saves the Copilot response
     */
    fun saveCopilotResponse(context: WorkspaceContext, response: String) {
        val responsePath = context.path.resolve("copilot-response.txt")
        Files.writeString(responsePath, response)
    }
    
    /**
     * Saves the diagnostic report
     */
    fun saveDiagnosticReport(context: WorkspaceContext, report: String) {
        val reportPath = context.path.resolve("diagnostic-report.md")
        Files.writeString(reportPath, report)
    }
    
    /**
     * Saves a copy of a modified file
     */
    fun saveModifiedFile(context: WorkspaceContext, filePath: String, content: String) {
        val changesDir = context.path.resolve("applied-changes")
        Files.createDirectories(changesDir)
        
        val fileName = Path.of(filePath).fileName.toString()
        val savedPath = changesDir.resolve(fileName)
        Files.writeString(savedPath, content)
    }
    
    /**
     * Appends to the activity log
     */
    fun logActivity(context: WorkspaceContext, message: String) {
        val logsDir = context.path.resolve("logs")
        Files.createDirectories(logsDir)
        
        val logPath = logsDir.resolve("activity.log")
        val timestamp = Instant.now()
        val logEntry = "[$timestamp] $message\n"
        
        Files.writeString(
            logPath,
            logEntry,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        )
    }
    
    /**
     * Lists all workspaces
     */
    fun listWorkspaces(): List<Path> {
        if (!Files.exists(workspaceRoot)) {
            return emptyList()
        }
        
        return Files.list(workspaceRoot).use { stream ->
            stream.filter { Files.isDirectory(it) }.toList()
        }
    }
}

/**
 * Context information for a remediation workspace
 */
data class WorkspaceContext(
    val path: Path,
    val className: String,
    val methodName: String,
    val timestamp: Instant
)
