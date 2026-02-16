package com.browser4tester.healer

import com.browser4tester.healer.graph.DEFAULT_UPDATE_STRATEGY
import com.browser4tester.healer.graph.GraphUpdateStrategy
import com.browser4tester.healer.graph.TestGraphBuilder
import com.browser4tester.healer.persistence.*
import com.browser4tester.healer.workspace.RemediationWorkspace
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

/**
 * Enhanced test orchestrator with persistent DAG-based tracking
 * 
 * This orchestrator:
 * - Builds and persists a test dependency graph
 * - Tracks execution history for all test methods
 * - Tracks remediation history for all AI fix attempts
 * - Creates dedicated workspaces for each remediation
 * - Archives complete audit trail
 */
class PersistentTestOrchestrator(
    private val projectRoot: Path,
    private val classExecutor: ClassExecutor,
    private val copilotAgent: CopilotAgent,
    private val patchApplier: PatchApplier,
    private val gitSnapshotManager: GitSnapshotManager,
    private val guard: TestIntegrityGuard,
    private val config: OrchestratorConfig,
    private val persistenceManager: PersistenceManager = PersistenceManager(),
    private val graphBuilder: TestGraphBuilder = TestGraphBuilder(projectRoot),
    private val updateStrategy: GraphUpdateStrategy = DEFAULT_UPDATE_STRATEGY,
    private val executionHistoryManager: ExecutionHistoryManager = ExecutionHistoryManager(),
    private val remediationHistoryManager: RemediationHistoryManager = RemediationHistoryManager(),
    private val workspace: RemediationWorkspace = RemediationWorkspace()
) {
    
    /**
     * Runs tests with full persistence tracking
     */
    fun run(testClasses: List<String>, classToFile: (String) -> Path): OrchestratorResult {
        val startTime = Instant.now()
        
        // 1. Load or build test graph
        val graph = loadOrBuildGraph()
        println("Graph loaded: ${graph.modules.size} modules, " +
                "${graph.modules.sumOf { it.testClasses.size }} test classes")
        
        // 2. Execute tests and track results
        val results = executeTestsWithTracking(testClasses, classToFile, graph)
        
        // 3. Save updated graph
        persistenceManager.saveGraph(results.updatedGraph)
        persistenceManager.archiveGraph(results.updatedGraph)
        
        val duration = java.time.Duration.between(startTime, Instant.now())
        println("\nOrchestrator completed in ${duration.seconds}s")
        println("- Total tests: ${results.classResults.size}")
        println("- Passed: ${results.classResults.count { it.passed }}")
        println("- Failed: ${results.classResults.count { !it.passed }}")
        println("- Remediation attempts: ${results.totalRemediationAttempts}")
        println("- Successful remediations: ${results.successfulRemediations}")
        
        return results
    }
    
    /**
     * Loads existing graph or builds new one based on update strategy
     */
    private fun loadOrBuildGraph(): TestGraph {
        val existingGraph = persistenceManager.loadGraph()
        
        if (updateStrategy.shouldUpdate(existingGraph, projectRoot)) {
            println("Building new test graph...")
            val newGraph = graphBuilder.buildGraph()
            
            // Merge history from existing graph if available
            val mergedGraph = if (existingGraph != null) {
                mergeGraphHistory(newGraph, existingGraph)
            } else {
                newGraph
            }
            
            persistenceManager.saveGraph(mergedGraph)
            return mergedGraph
        }
        
        return existingGraph ?: run {
            println("No existing graph, building new one...")
            val newGraph = graphBuilder.buildGraph()
            persistenceManager.saveGraph(newGraph)
            newGraph
        }
    }
    
    /**
     * Merges execution and remediation history from old graph into new graph
     */
    private fun mergeGraphHistory(newGraph: TestGraph, oldGraph: TestGraph): TestGraph {
        val moduleMap = oldGraph.modules.associateBy { it.id }
        
        val mergedModules = newGraph.modules.map modules@{ newModule ->
            val oldModule = moduleMap[newModule.id]
            if (oldModule == null) return@modules newModule
            
            val classMap = oldModule.testClasses.associateBy { it.id }
            val mergedClasses = newModule.testClasses.map classes@{ newClass ->
                val oldClass = classMap[newClass.id]
                if (oldClass == null) return@classes newClass
                
                val methodMap = oldClass.testMethods.associateBy { it.id }
                val mergedMethods = newClass.testMethods.map methods@{ newMethod ->
                    val oldMethod = methodMap[newMethod.id]
                    if (oldMethod == null) return@methods newMethod
                    
                    // Transfer history
                    newMethod.copy(
                        lastExecution = oldMethod.lastExecution,
                        lastRemediation = oldMethod.lastRemediation,
                        executionHistory = oldMethod.executionHistory,
                        remediationHistory = oldMethod.remediationHistory
                    )
                }
                
                newClass.copy(testMethods = mergedMethods)
            }
            
            newModule.copy(testClasses = mergedClasses)
        }
        
        return newGraph.copy(modules = mergedModules)
    }
    
    /**
     * Executes tests and tracks all execution/remediation in the graph
     */
    private fun executeTestsWithTracking(
        testClasses: List<String>,
        classToFile: (String) -> Path,
        initialGraph: TestGraph
    ): OrchestratorResult {
        var currentGraph = initialGraph
        val classResults = mutableListOf<ClassExecutionResult>()
        var totalRemediationAttempts = 0
        var successfulRemediations = 0
        
        for (clazz in testClasses.sorted()) {
            println("\n=== Executing: $clazz ===")
            
            // Execute test class
            var result = classExecutor.execute(clazz)
            
            // Record execution for each method
            currentGraph = recordExecutionResults(currentGraph, clazz, result)
            
            if (result.passed) {
                println("✓ All tests passed")
                classResults += result
                continue
            }
            
            // Attempt remediation
            val testFile = classToFile(clazz)
            val beforeFix = Files.readString(testFile)
            gitSnapshotManager.snapshot("pre-ai-fix snapshot for $clazz")
            
            var repaired = false
            repeat(config.maxRetryPerClass) { attemptNumber ->
                println("Remediation attempt ${attemptNumber + 1}/${config.maxRetryPerClass}...")
                totalRemediationAttempts++
                
                // Create dedicated workspace for this remediation
                val workspaceContext = workspace.createWorkspace(
                    className = clazz,
                    methodName = result.failures.first().method,
                    timestamp = Instant.now()
                )
                
                // Save failure context
                workspace.saveFailureContext(
                    context = workspaceContext,
                    errorMessage = result.failures.first().message,
                    stackTrace = result.failures.first().stacktrace,
                    testSource = beforeFix
                )
                
                workspace.logActivity(workspaceContext, "Starting remediation attempt ${attemptNumber + 1}")
                
                // Attempt repair
                val remediationStart = Instant.now()
                val context = RepairContext(
                    className = clazz,
                    testFile = testFile,
                    testSource = Files.readString(testFile),
                    failures = result.failures,
                )
                
                val repair = copilotAgent.repair(context)
                
                // Save Copilot artifacts
                workspace.saveCopilotPrompt(workspaceContext, "AI repair request for ${result.failures.size} failing tests")
                workspace.saveCopilotResponse(workspaceContext, repair.rawOutput)
                workspace.saveDiagnosticReport(workspaceContext, "Remediation attempt ${attemptNumber + 1}")
                
                guard.verify(beforeFix, repair.updatedFileContent)
                patchApplier.apply(testFile, repair.updatedFileContent)
                gitSnapshotManager.stage(testFile.toString())
                
                // Save modified file to workspace
                workspace.saveModifiedFile(workspaceContext, testFile.toString(), repair.updatedFileContent)
                
                val remediationDuration = java.time.Duration.between(remediationStart, Instant.now())
                
                // Re-execute test
                result = classExecutor.execute(clazz)
                
                // Record remediation
                val remediationResult = if (result.passed) RemediationResult.SUCCESS else RemediationResult.FAILURE
                currentGraph = recordRemediationAttempt(
                    graph = currentGraph,
                    className = clazz,
                    methodName = result.failures.firstOrNull()?.method ?: "unknown",
                    result = remediationResult,
                    workspacePath = workspaceContext.path.toString(),
                    durationMs = remediationDuration.toMillis()
                )
                
                // Record new execution
                currentGraph = recordExecutionResults(currentGraph, clazz, result)
                
                workspace.logActivity(workspaceContext, "Remediation ${if (result.passed) "succeeded" else "failed"}")
                
                if (result.passed) {
                    println("✓ Remediation successful!")
                    repaired = true
                    successfulRemediations++
                    return@repeat
                } else {
                    println("✗ Still failing: ${result.failures.size} test(s)")
                }
            }
            
            if (!repaired) {
                println("✗ Failed to repair after ${config.maxRetryPerClass} attempts")
                gitSnapshotManager.rollbackLastCommit()
            }
            
            classResults += result
        }
        
        return OrchestratorResult(
            classResults = classResults,
            updatedGraph = currentGraph,
            totalRemediationAttempts = totalRemediationAttempts,
            successfulRemediations = successfulRemediations
        )
    }
    
    /**
     * Records execution results for all test methods in a class
     */
    private fun recordExecutionResults(
        graph: TestGraph,
        className: String,
        result: ClassExecutionResult
    ): TestGraph {
        var updatedGraph = graph
        
        // Find all test methods in this class and record SUCCESS for passing tests
        // For failing tests, record FAILURE
        val failureMap = result.failures.associateBy { it.method }
        
        // Find the module and class in the graph
        graph.modules.forEach { module ->
            module.testClasses.forEach { testClass ->
                if (testClass.fullyQualifiedName == className) {
                    testClass.testMethods.forEach { method ->
                        val failure = failureMap[method.name]
                        val executionResult = if (failure != null) {
                            ExecutionResult.FAILURE
                        } else {
                            ExecutionResult.SUCCESS
                        }
                        
                        updatedGraph = persistenceManager.updateMethodNode(
                            graph = updatedGraph,
                            moduleId = module.id,
                            classId = testClass.id,
                            methodId = method.id
                        ) { methodNode ->
                            executionHistoryManager.recordExecution(
                                methodNode = methodNode,
                                result = executionResult,
                                durationMs = 0, // ClassExecutor doesn't provide per-method duration
                                errorMessage = failure?.message,
                                stackTrace = failure?.stacktrace
                            )
                        }
                    }
                }
            }
        }
        
        return updatedGraph
    }
    
    /**
     * Records a remediation attempt in the graph
     */
    private fun recordRemediationAttempt(
        graph: TestGraph,
        className: String,
        methodName: String,
        result: RemediationResult,
        workspacePath: String,
        durationMs: Long
    ): TestGraph {
        var updatedGraph = graph
        
        // Find the method in the graph
        graph.modules.forEach { module ->
            module.testClasses.forEach { testClass ->
                if (testClass.fullyQualifiedName == className) {
                    testClass.testMethods.forEach { method ->
                        if (method.name == methodName) {
                            updatedGraph = persistenceManager.updateMethodNode(
                                graph = updatedGraph,
                                moduleId = module.id,
                                classId = testClass.id,
                                methodId = method.id
                            ) { methodNode ->
                                remediationHistoryManager.recordRemediation(
                                    methodNode = methodNode,
                                    result = result,
                                    diagnosticReport = "AI remediation attempt",
                                    workspacePath = workspacePath,
                                    copilotPrompt = "Fix failing test",
                                    copilotResponse = "AI response",
                                    changesApplied = emptyList(),
                                    durationMs = durationMs
                                )
                            }
                        }
                    }
                }
            }
        }
        
        return updatedGraph
    }
}

/**
 * Result of running the orchestrator with persistence
 */
data class OrchestratorResult(
    val classResults: List<ClassExecutionResult>,
    val updatedGraph: TestGraph,
    val totalRemediationAttempts: Int,
    val successfulRemediations: Int
) {
    val allPassed: Boolean = classResults.all { it.passed }
}
