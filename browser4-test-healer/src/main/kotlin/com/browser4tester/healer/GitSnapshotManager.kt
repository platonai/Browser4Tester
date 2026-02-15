package com.browser4tester.healer

class GitSnapshotManager {
    fun snapshot(message: String) {
        run("git", "add", "-A")
        // Only commit if there are changes
        val status = runCaptureOutput("git", "status", "--porcelain")
        if (status.isNotBlank()) {
            run("git", "commit", "-m", message)
        }
    }

    fun stage(path: String) {
        run("git", "add", path)
    }

    fun rollbackLastCommit() {
        run("git", "reset", "--hard", "HEAD~1")
    }

    private fun run(vararg command: String) {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        require(exit == 0) { "Command failed: ${command.joinToString(" ")}\n$output" }
    }

    private fun runCaptureOutput(vararg command: String): String {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output
    }
}
