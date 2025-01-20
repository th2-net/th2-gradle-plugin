/*
 * Copyright 2024-2025 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.gradle

import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GrpcTh2PluginTest {
    @Test
    fun `configure correct dependencies between java compile and proto tasks`() {
        // Create a test project and apply the plugin
        val project =
            ProjectBuilder.builder()
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
            },
        )
    }

    @Test
    fun `does not add service generator to dependencies if it is not enabled`() {
        // Create a test project and apply the plugin
        val project =
            ProjectBuilder.builder()
                .build()

        project.plugins.apply("java-library")
        project.plugins.apply("com.exactpro.th2.gradle.grpc")

        (project as DefaultProject).evaluate()

        val serviceGenerator =
            project.configurations.getByName("implementation")
                .allDependencies
                .find {
                    it.group == "com.exactpro.th2" && it.name == "grpc-service-generator"
                }
        assertNull(serviceGenerator, "service generator should not be added to dependencies")
    }

    @Test
    fun `adds service generator to dependencies if it is enabled`() {
        // Create a test project and apply the plugin
        val project =
            ProjectBuilder.builder()
                .build()

        project.plugins.apply("java-library")
        project.plugins.apply("com.exactpro.th2.gradle.grpc")

        project.the<GrpcTh2Extension>().service.set(true)

        (project as DefaultProject).evaluate()

        val serviceGenerator =
            project.configurations.getByName("implementation")
                .allDependencies
                .find {
                    it.group == "com.exactpro.th2" && it.name == "grpc-service-generator"
                }
        assertNotNull(serviceGenerator, "service generator should be added to dependencies")
    }

    @Test
    fun `reports error if not a java-library plugin is applied`() {
        val project =
            ProjectBuilder.builder()
                .build()

        project.plugins.apply("java")

        val ex =
            assertThrows<Exception> {
                project.plugins.apply("com.exactpro.th2.gradle.grpc")
            }

        assertEquals(
            "java-library plugin must be applied to the project with gRPC but a different one was",
            ex.cause?.message,
            "unexpected error",
        )
    }
}
