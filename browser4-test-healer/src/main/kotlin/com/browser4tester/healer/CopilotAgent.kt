package com.browser4tester.healer


class CopilotAgent {

    fun repair(context: RepairContext): CopilotRepairResult {
        val prompt = buildPrompt(context)
        val process = ProcessBuilder("gh", "copilot", "--", "-p", prompt, "--allow-all-tools")
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        require(exit == 0) { "Copilot CLI failed with exit $exit: $output" }

        val extractedCode = extractCodeFromOutput(output)
        require(extractedCode.isNotBlank()) { "Copilot returned empty code. Output: $output" }

        return CopilotRepairResult(
            updatedFileContent = extractedCode,
            rawOutput = output,
        )
    }

    private fun extractCodeFromOutput(output: String): String {
        // Try to extract code from markdown code blocks
        val codeBlockRegex = Regex("```(?:kotlin)?\\s*\\n([\\s\\S]*?)```", RegexOption.MULTILINE)
        val match = codeBlockRegex.find(output)
        
        if (match != null) {
            return match.groupValues[1].trim()
        }
        
        // If no code block found, try to extract everything between "package" and the statistics section
        val packageStart = output.indexOf("package ")
        if (packageStart != -1) {
            val statsStart = output.indexOf("\nTotal usage est:", packageStart)
            val endIndex = if (statsStart != -1) statsStart else output.length
            return output.substring(packageStart, endIndex).trim()
        }
        
        // Fallback: return cleaned output
        val statsStart = output.indexOf("\nTotal usage est:")
        return if (statsStart != -1) {
            output.substring(0, statsStart).trim()
        } else {
            output.trim()
        }
    }

    private fun buildPrompt(context: RepairContext): String {
        val failures = context.failures.joinToString("\n\n") {
            """
            Method: ${it.method}
            Error: ${it.message}
            
            Stack Trace:
            ${it.stacktrace.take(500)}
            """.trimIndent()
        }

        return """
Fix the failing Kotlin test class. Return ONLY the corrected file content, nothing else.

Test Class: ${context.className}
File Path: ${context.testFile}

Current Source Code:
```kotlin
${context.testSource}
```

Test Failures:
$failures

Requirements:
1. Output ONLY the complete fixed Kotlin source code
2. Do not add explanations, markdown formatting, or statistics
3. Do not create new files - just output the corrected source
4. Keep all original test methods and assertions
5. Do not weaken tests by removing assertions
6. Fix only the test code, not production code

Output format: Plain Kotlin source code only, starting with 'package' statement.
""".trimIndent()
    }
}
