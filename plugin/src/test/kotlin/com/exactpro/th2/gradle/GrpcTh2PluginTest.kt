package com.exactpro.th2.gradle

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GrpcTh2PluginTest {
    @Test
    fun `configure correct dependencies between java compile and proto tasks`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder()
            .build()

        project.plugins.apply("java")
        project.plugins.apply("com.exactpro.th2.gradle.grpc")

        val generateProtoTask = project.tasks.findByName("generateProto")
        assertNotNull(generateProtoTask) { "cannot find generateProto task" }

        assertTrue(
            project.tasks.getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME).dependsOn.contains(generateProtoTask),
            "${JavaPlugin.COMPILE_JAVA_TASK_NAME} does not depend on generateProto",
        )

        assertTrue(
            project.tasks.getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).dependsOn.contains(generateProtoTask),
            "${JavaPlugin.PROCESS_RESOURCES_TASK_NAME} does not depend on generateProto",
        )
    }
}
