package com.exactpro.th2.gradle

import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GrpcTh2PluginTest {
    @Test
    fun `configure correct dependencies between java compile and proto tasks`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder()
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply("com.exactpro.th2.gradle.grpc")

        (project as DefaultProject).evaluate()

        val generateProtoTask = project.tasks.findByName("generateProto")
        assertAll(
            { assertNotNull(generateProtoTask) { "cannot find generateProto task" } },

            {
                assertTrue(
                    project.tasks.getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME).dependsOn.contains(generateProtoTask),
                    "${JavaPlugin.COMPILE_JAVA_TASK_NAME} does not depend on generateProto",
                )
            },

            {
                assertTrue(
                    project.tasks.getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).dependsOn.contains(generateProtoTask),
                    "${JavaPlugin.PROCESS_RESOURCES_TASK_NAME} does not depend on generateProto",
                )
            }
        )
    }

    @Test
    fun `reports error if not a java-library plugin is applied`() {
        val project = ProjectBuilder.builder()
            .build()

        project.plugins.apply("java")

        val ex = assertThrows<Exception> {
            project.plugins.apply("com.exactpro.th2.gradle.grpc")
        }

        assertEquals(
            "java-library plugin must be applied to the project with gRPC but a different one was",
            ex.cause?.message,
            "unexpected error",
        )
    }
}
