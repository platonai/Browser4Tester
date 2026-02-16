package com.browser4tester.healer

import java.nio.file.Path

/**
 * Main entry point using the persistent DAG-based orchestrator
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: java -jar healer.jar [--project-root <path>] <test-class-1> <test-class-2> ...")
        println("  --project-root: Root directory of the project (default: current directory)")
        println("  test-class: Fully-qualified test class names to execute")
        kotlin.system.exitProcess(1)
    }
    
    // Parse arguments
    var projectRoot = Path.of(".")
    val testClasses = mutableListOf<String>()
    
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--project-root" -> {
                if (i + 1 >= args.size) {
                    println("Error: --project-root requires a path argument")
                    kotlin.system.exitProcess(1)
                }
                projectRoot = Path.of(args[i + 1])
                i += 2
            }
            else -> {
                testClasses.add(args[i])
                i++
            }
        }
    }
    
    if (testClasses.isEmpty()) {
        println("Error: At least one test class must be specified")
        kotlin.system.exitProcess(1)
    }
    
    println("=== Persistent Self-Healing Test Orchestrator ===")
    println("Project Root: $projectRoot")
    println("Test Classes: ${testClasses.joinToString(", ")}")
    println()
    
    val orchestrator = PersistentTestOrchestrator(
        projectRoot = projectRoot,
        classExecutor = ClassExecutor(),
        copilotAgent = CopilotAgent(),
        patchApplier = PatchApplier(),
        gitSnapshotManager = GitSnapshotManager(),
        guard = TestIntegrityGuard(),
        config = OrchestratorConfig()
    )
    
    val result = orchestrator.run(testClasses) { className ->
        val relative = "src/test/kotlin/" + className.replace('.', '/') + ".kt"
        projectRoot.resolve(relative)
    }
    
    println("\n=== Summary ===")
    if (result.allPassed) {
        println("✓ All tests passed!")
    } else {
        println("✗ Some tests failed:")
        result.classResults.filterNot { it.passed }.forEach { classResult ->
            println("  - ${classResult.className}: ${classResult.failures.size} failure(s)")
        }
    }
    
    println("\nPersistence Location: .browser4tester/")
    println("- test-graph.json: Current test graph")
    println("- archives/: Historical graph snapshots")
    println("- remediation/: Detailed remediation workspaces")
    
    if (!result.allPassed) {
        kotlin.system.exitProcess(1)
    }
}
