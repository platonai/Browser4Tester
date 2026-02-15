package com.browser4tester.healer

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TestIntegrityGuardTest {

    private val guard = TestIntegrityGuard()

    @Test
    fun `accepts equivalent or stronger test`() {
        val original = """
            @Test
            fun t() {
                assertEquals(1, 1)
            }
        """.trimIndent()

        val updated = """
            @Test
            fun t() {
                assertEquals(1, 1)
                assertTrue(2 > 1)
            }
        """.trimIndent()

        guard.verify(original, updated)
    }

    @Test
    fun `rejects dropped assertions`() {
        val original = "assertEquals(1, 1)"
        val updated = "println(\"no assertions\")"

        assertThrows(IllegalArgumentException::class.java) {
            guard.verify(original, updated)
        }
    }
}
