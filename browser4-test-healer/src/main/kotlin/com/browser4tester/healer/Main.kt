package com.browser4tester.healer

fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "Provide at least one fully-qualified test class name." }

    val orchestrator = TestOrchestrator(
        classExecutor = ClassExecutor(),
        copilotAgent = CopilotAgent(),
        patchApplier = PatchApplier(),
        gitSnapshotManager = GitSnapshotManager(),
        guard = TestIntegrityGuard(),
        config = OrchestratorConfig(),
    )

    val results = orchestrator.run(args.toList()) { className ->
        val relative = "src/test/kotlin/" + className.replace('.', '/') + ".kt"
        java.nio.file.Path.of(relative)
    }

    val failed = results.filterNot { it.passed }
    if (failed.isEmpty()) {
        println("All classes passed.")
    } else {
        println("Unstable classes:")
        failed.forEach { println("- ${it.className}") }
        kotlin.system.exitProcess(1)
    }
}
