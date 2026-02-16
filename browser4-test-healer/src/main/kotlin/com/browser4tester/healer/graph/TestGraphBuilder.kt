package com.browser4tester.healer.graph

import com.browser4tester.healer.persistence.*
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.TestIdentifier
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.isRegularFile

/**
 * Builds the test dependency graph by discovering modules, classes, and methods
 */
class TestGraphBuilder(
    private val projectRoot: Path
) {
    
    /**
     * Builds a complete test graph from the project
     */
    fun buildGraph(): TestGraph {
        val modules = discoverModules()
        return TestGraph(
            modules = modules,
            lastUpdated = Instant.now()
        )
    }
    
    /**
     * Discovers all Maven modules in the project
     */
    private fun discoverModules(): List<ModuleNode> {
        val modules = mutableListOf<ModuleNode>()
        
        // Find all pom.xml files to identify modules
        Files.walk(projectRoot, 5).use { paths ->
            paths
                .filter { it.fileName.toString() == "pom.xml" }
                .filter { it != projectRoot.resolve("pom.xml") } // Exclude parent pom
                .forEach { pomPath ->
                    val modulePath = pomPath.parent
                    val module = buildModuleNode(modulePath)
                    if (module != null) {
                        modules.add(module)
                    }
                }
        }
        
        return modules
    }
    
    /**
     * Builds a module node from a module directory
     */
    private fun buildModuleNode(modulePath: Path): ModuleNode? {
        val moduleId = projectRoot.relativize(modulePath).toString()
        val moduleName = modulePath.fileName.toString()
        
        // Discover test classes in this module
        val testClasses = discoverTestClasses(modulePath)
        
        if (testClasses.isEmpty()) {
            return null // Skip modules with no tests
        }
        
        // Parse dependencies from pom.xml (simplified - could be enhanced)
        val dependencies = parseMavenDependencies(modulePath)
        
        return ModuleNode(
            id = moduleId,
            name = moduleName,
            path = modulePath.toString(),
            dependencies = dependencies,
            testClasses = testClasses
        )
    }
    
    /**
     * Discovers test classes in a module
     */
    private fun discoverTestClasses(modulePath: Path): List<TestClassNode> {
        val testClasses = mutableListOf<TestClassNode>()
        val testSourceRoot = modulePath.resolve("src/test/kotlin")
        
        if (!Files.exists(testSourceRoot)) {
            return emptyList()
        }
        
        // Find all test files
        Files.walk(testSourceRoot).use { paths ->
            paths
                .filter { it.isRegularFile() }
                .filter { it.fileName.toString().endsWith(".kt") }
                .filter { it.fileName.toString().contains("Test") }
                .forEach { testFile ->
                    val testClass = buildTestClassNode(testFile, testSourceRoot, modulePath)
                    if (testClass != null) {
                        testClasses.add(testClass)
                    }
                }
        }
        
        return testClasses
    }
    
    /**
     * Builds a test class node from a test file
     */
    private fun buildTestClassNode(
        testFile: Path,
        testSourceRoot: Path,
        modulePath: Path
    ): TestClassNode? {
        val relativePath = testSourceRoot.relativize(testFile)
        val fqn = relativePath.toString()
            .removeSuffix(".kt")
            .replace("/", ".")
        
        // Use JUnit Platform to discover test methods
        val testMethods = discoverTestMethods(fqn)
        
        if (testMethods.isEmpty()) {
            return null // Skip classes with no test methods
        }
        
        return TestClassNode(
            id = "$modulePath/$fqn",
            fullyQualifiedName = fqn,
            filePath = testFile.toString(),
            testMethods = testMethods
        )
    }
    
    /**
     * Discovers test methods in a class using JUnit Platform
     */
    private fun discoverTestMethods(className: String): List<TestMethodNode> {
        return try {
            val launcher = LauncherFactory.create()
            val request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(className))
                .build()
            
            val testPlan = launcher.discover(request)
            val methods = mutableListOf<TestMethodNode>()
            
            testPlan.roots.forEach { root ->
                collectTestMethods(testPlan, root, className, methods)
            }
            
            methods
        } catch (e: Exception) {
            // Class might not be loadable yet (not compiled)
            emptyList()
        }
    }
    
    private fun collectTestMethods(
        testPlan: org.junit.platform.launcher.TestPlan,
        testIdentifier: TestIdentifier,
        className: String,
        methods: MutableList<TestMethodNode>
    ) {
        if (testIdentifier.isTest) {
            methods.add(
                TestMethodNode(
                    id = testIdentifier.uniqueId,
                    name = testIdentifier.legacyReportingName ?: testIdentifier.displayName,
                    displayName = testIdentifier.displayName
                )
            )
        }
        
        testPlan.getChildren(testIdentifier).forEach { child ->
            collectTestMethods(testPlan, child, className, methods)
        }
    }
    
    /**
     * Parses Maven dependencies from pom.xml (simplified implementation)
     */
    private fun parseMavenDependencies(modulePath: Path): List<String> {
        // Simplified: Execute maven command to get dependency tree
        // In production, you'd want to use a proper Maven API or parse the pom.xml
        return try {
            val pomFile = modulePath.resolve("pom.xml")
            if (!Files.exists(pomFile)) {
                return emptyList()
            }
            
            // For now, return empty - this could be enhanced to parse actual dependencies
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
