package com.browser4tester.healer


class CopilotAgent {

    fun repair(context: RepairContext): CopilotRepairResult {
        val prompt = buildPrompt(context)
        val process = ProcessBuilder("gh", "copilot", "suggest", "-p", prompt)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        require(exit == 0) { "Copilot CLI failed with exit $exit: $output" }

        return CopilotRepairResult(
            updatedFileContent = output.trim(),
            rawOutput = output,
        )
    }

    private fun buildPrompt(context: RepairContext): String {
        val failures = context.failures.joinToString("\n") {
            "Method: ${it.method}\nMessage: ${it.message}\nStacktrace:\n${it.stacktrace}"
        }

        return """
You are fixing a failing JUnit 5 Kotlin test class.

Target class: ${context.className}

Test class source:
${context.testSource}

Failure report:
${failures}

Constraints:
- Modify ONLY this test file.
- Keep test intent and assertions meaningful.
- Never weaken by replacing assertions with tautologies.
- Return FULL updated file content only.
""".trimIndent()
    }
}
