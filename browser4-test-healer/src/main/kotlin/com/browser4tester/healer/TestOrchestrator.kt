package com.browser4tester.healer

import java.nio.file.Files
import java.nio.file.Path

class TestOrchestrator(
    private val classExecutor: ClassExecutor,
    private val copilotAgent: CopilotAgent,
    private val patchApplier: PatchApplier,
    private val gitSnapshotManager: GitSnapshotManager,
    private val guard: TestIntegrityGuard,
    private val config: OrchestratorConfig,
) {

    fun run(testClasses: List<String>, classToFile: (String) -> Path): List<ClassExecutionResult> {
        val results = mutableListOf<ClassExecutionResult>()

        for (clazz in testClasses.sorted()) {
            var result = classExecutor.execute(clazz)
            if (result.passed) {
                results += result
                continue
            }

            val testFile = classToFile(clazz)
            val beforeFix = Files.readString(testFile)
            gitSnapshotManager.snapshot("pre-ai-fix snapshot for $clazz")

            var repaired = false
            repeat(config.maxRetryPerClass) {
                val context = RepairContext(
                    className = clazz,
                    testFile = testFile,
                    testSource = Files.readString(testFile),
                    failures = result.failures,
                )

                val repair = copilotAgent.repair(context)
                guard.verify(beforeFix, repair.updatedFileContent)
                patchApplier.apply(testFile, repair.updatedFileContent)
                gitSnapshotManager.stage(testFile.toString())

                result = classExecutor.execute(clazz)
                if (result.passed) {
                    repaired = true
                    return@repeat
                }
            }

            if (!repaired) {
                gitSnapshotManager.rollbackLastCommit()
            }

            results += result
        }

        return results
    }
}
