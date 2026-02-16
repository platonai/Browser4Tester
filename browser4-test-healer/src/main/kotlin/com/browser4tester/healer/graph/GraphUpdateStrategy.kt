package com.browser4tester.healer.graph

import com.browser4tester.healer.persistence.TestGraph
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

/**
 * Strategies for determining when to refresh the test dependency graph
 */
sealed class GraphUpdateStrategy {
    abstract fun shouldUpdate(
        existingGraph: TestGraph?,
        projectRoot: Path
    ): Boolean
    
    /**
     * Always refresh the graph on each execution
     */
    object Always : GraphUpdateStrategy() {
        override fun shouldUpdate(existingGraph: TestGraph?, projectRoot: Path): Boolean = true
    }
    
    /**
     * Never refresh the graph unless it doesn't exist
     */
    object Never : GraphUpdateStrategy() {
        override fun shouldUpdate(existingGraph: TestGraph?, projectRoot: Path): Boolean {
            return existingGraph == null
        }
    }
    
    /**
     * Refresh if the graph is older than a specified duration
     */
    class TimeBasedStrategy(
        private val maxAge: Duration = Duration.ofHours(24)
    ) : GraphUpdateStrategy() {
        override fun shouldUpdate(existingGraph: TestGraph?, projectRoot: Path): Boolean {
            if (existingGraph == null) return true
            
            val age = Duration.between(existingGraph.lastUpdated, Instant.now())
            return age > maxAge
        }
    }
    
    /**
     * Refresh if any test files have been modified since last update
     */
    class FileModificationStrategy : GraphUpdateStrategy() {
        override fun shouldUpdate(existingGraph: TestGraph?, projectRoot: Path): Boolean {
            if (existingGraph == null) return true
            
            // Check if any test files have been modified
            val lastUpdate = existingGraph.lastUpdated
            
            Files.walk(projectRoot, 10).use { paths ->
                return paths
                    .filter { it.fileName.toString().endsWith(".kt") }
                    .filter { it.toString().contains("/src/test/") }
                    .anyMatch { testFile ->
                        try {
                            val lastModified = Files.getLastModifiedTime(testFile).toInstant()
                            lastModified.isAfter(lastUpdate)
                        } catch (e: Exception) {
                            false
                        }
                    }
            }
        }
    }
    
    /**
     * Refresh if pom.xml files have been modified (indicates structural changes)
     */
    class PomModificationStrategy : GraphUpdateStrategy() {
        override fun shouldUpdate(existingGraph: TestGraph?, projectRoot: Path): Boolean {
            if (existingGraph == null) return true
            
            val lastUpdate = existingGraph.lastUpdated
            
            Files.walk(projectRoot, 5).use { paths ->
                return paths
                    .filter { it.fileName.toString() == "pom.xml" }
                    .anyMatch { pomFile ->
                        try {
                            val lastModified = Files.getLastModifiedTime(pomFile).toInstant()
                            lastModified.isAfter(lastUpdate)
                        } catch (e: Exception) {
                            false
                        }
                    }
            }
        }
    }
    
    /**
     * Combines multiple strategies - updates if ANY strategy returns true
     */
    class CompositeStrategy(
        private val strategies: List<GraphUpdateStrategy>
    ) : GraphUpdateStrategy() {
        override fun shouldUpdate(existingGraph: TestGraph?, projectRoot: Path): Boolean {
            return strategies.any { it.shouldUpdate(existingGraph, projectRoot) }
        }
    }
}

/**
 * Default strategy: Update if graph doesn't exist, or pom.xml modified, or older than 24h
 */
val DEFAULT_UPDATE_STRATEGY = GraphUpdateStrategy.CompositeStrategy(
    listOf(
        GraphUpdateStrategy.PomModificationStrategy(),
        GraphUpdateStrategy.TimeBasedStrategy(Duration.ofHours(24))
    )
)
