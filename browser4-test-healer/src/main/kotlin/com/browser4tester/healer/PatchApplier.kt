package com.browser4tester.healer

import java.nio.file.Files
import java.nio.file.Path

class PatchApplier {
    fun apply(testFile: Path, repairedContent: String) {
        Files.writeString(testFile, repairedContent)
    }
}

class TestIntegrityGuard {
    fun verify(original: String, updated: String) {
        val originalAssertions = Regex("\\bassert[A-Za-z]+\\(").findAll(original).count()
        val updatedAssertions = Regex("\\bassert[A-Za-z]+\\(").findAll(updated).count()
        require(updatedAssertions >= originalAssertions) {
            "Integrity violation: assertion count dropped from $originalAssertions to $updatedAssertions"
        }

        val originalTests = Regex("@Test").findAll(original).count()
        val updatedTests = Regex("@Test").findAll(updated).count()
        require(updatedTests >= originalTests) {
            "Integrity violation: @Test count dropped from $originalTests to $updatedTests"
        }

        require(!Regex("assertTrue\\(\\s*true\\s*\\)").containsMatchIn(updated)) {
            "Integrity violation: tautological assertTrue(true) detected"
        }
    }
}
