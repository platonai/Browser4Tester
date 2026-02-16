package com.browser4tester.healer.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Manages persistence of the test graph to/from JSON files
 */
class PersistenceManager(
    private val storageRoot: Path = Path.of(".browser4tester")
) {
    private val graphFile = storageRoot.resolve("test-graph.json")
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
        enable(SerializationFeature.INDENT_OUTPUT)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    
    init {
        Files.createDirectories(storageRoot)
    }
    
    /**
     * Saves the test graph to persistent storage
     */
    fun saveGraph(graph: TestGraph) {
        val json = objectMapper.writeValueAsString(graph)
        Files.writeString(
            graphFile,
            json,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }
    
    /**
     * Loads the test graph from persistent storage
     * Returns null if no graph exists
     */
    fun loadGraph(): TestGraph? {
        if (!Files.exists(graphFile)) {
            return null
        }
        
        return try {
            val json = Files.readString(graphFile)
            objectMapper.readValue(json, TestGraph::class.java)
        } catch (e: Exception) {
            println("Warning: Failed to load test graph: ${e.message}")
            null
        }
    }
    
    /**
     * Checks if a persisted graph exists
     */
    fun graphExists(): Boolean = Files.exists(graphFile)
    
    /**
     * Archives the current graph with a timestamp
     */
    fun archiveGraph(graph: TestGraph) {
        val timestamp = graph.lastUpdated.toString().replace(":", "-")
        val archiveFile = storageRoot.resolve("archives")
            .also { Files.createDirectories(it) }
            .resolve("test-graph-$timestamp.json")
        
        val json = objectMapper.writeValueAsString(graph)
        Files.writeString(archiveFile, json)
    }
    
    /**
     * Updates a specific test method node in the graph
     */
    fun updateMethodNode(
        graph: TestGraph,
        moduleId: String,
        classId: String,
        methodId: String,
        updater: (TestMethodNode) -> TestMethodNode
    ): TestGraph {
        val updatedModules = graph.modules.map { module ->
            if (module.id == moduleId) {
                val updatedClasses = module.testClasses.map { testClass ->
                    if (testClass.id == classId) {
                        val updatedMethods = testClass.testMethods.map { method ->
                            if (method.id == methodId) {
                                updater(method)
                            } else {
                                method
                            }
                        }
                        testClass.copy(testMethods = updatedMethods)
                    } else {
                        testClass
                    }
                }
                module.copy(testClasses = updatedClasses)
            } else {
                module
            }
        }
        
        return graph.copy(modules = updatedModules)
    }
    
    /**
     * Finds a test method node by its ID path
     */
    fun findMethodNode(
        graph: TestGraph,
        moduleId: String,
        classId: String,
        methodId: String
    ): TestMethodNode? {
        return graph.modules
            .find { it.id == moduleId }
            ?.testClasses
            ?.find { it.id == classId }
            ?.testMethods
            ?.find { it.id == methodId }
    }
}
