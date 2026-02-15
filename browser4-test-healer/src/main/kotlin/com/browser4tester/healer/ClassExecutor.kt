package com.browser4tester.healer

import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.Launcher
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import java.io.PrintWriter
import java.io.StringWriter

class FailureCollector : TestExecutionListener {
    private val failures = mutableListOf<FailureDetail>()

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: org.junit.platform.engine.TestExecutionResult) {
        if (testIdentifier.isTest && testExecutionResult.status == org.junit.platform.engine.TestExecutionResult.Status.FAILED) {
            val throwable = testExecutionResult.throwable.orElse(null)
            failures += FailureDetail(
                method = testIdentifier.displayName,
                message = throwable?.message ?: "Unknown failure",
                stacktrace = throwable?.toStacktrace().orEmpty(),
            )
        }
    }

    fun resultFor(className: String): ClassExecutionResult = ClassExecutionResult(className, failures.toList())
}

class ClassExecutor(private val launcher: Launcher = LauncherFactory.create()) {

    fun execute(className: String): ClassExecutionResult {
        val collector = FailureCollector()
        val request = org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClass(className))
            .build()

        launcher.execute(request, collector)
        return collector.resultFor(className)
    }
}

private fun Throwable.toStacktrace(): String {
    val writer = StringWriter()
    printStackTrace(PrintWriter(writer))
    return writer.toString()
}
