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

import org.gradle.api.GradleException
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaApplication
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComponentTh2PluginTest {
    @Test
    fun `applies all required plugins`() {
        val project =
            ProjectBuilder.builder()
                .build()

        project.pluginManager.apply(ComponentTh2Plugin::class.java)

        assertAll(
            {
                assertTrue(
                    project.plugins.hasPlugin(ApplicationPlugin::class.java),
                    "application plugin was not applied",
                )
            },
            {
                assertNotNull(
                    project.extensions.findByName("docker"),
                    "no 'docker' extension found",
                ) {
                    assertIs<DockerTh2Extension>(it, "incorrect extension type")
                }
            },
            {
                assertTrue(
                    project.tasks.getByName("dockerPrepare").dependsOn
                        .contains(project.tasks.getByName("installDist")),
                    "installDist dependency was not added to docker task",
                )
            },
            {
                assertEquals(
                    "service",
                    project.the<JavaApplication>().applicationName,
                    "unexpected application name",
                )
            },
        )
    }

    @Test
    fun `main class is specified`() {
        val project =
            ProjectBuilder.builder()
                .build()
        project.version = "1.0.0"
        project.pluginManager.apply(ComponentTh2Plugin::class.java)
        project.the<JavaApplication>().mainClass.set("com.exactpro.th2.Main")

        // We need to think how to reorganize plugins to avoid calling evaluation in tests
        (project as DefaultProject).evaluate()
    }

    @Test
    fun `main class isn't specified`() {
        val project =
            ProjectBuilder.builder()
                .build()
        project.version = "1.0.0"
        project.pluginManager.apply(ComponentTh2Plugin::class.java)

        assertThrows<GradleException> {
            // We need to think how to reorganize plugins to avoid calling evaluation in tests
            (project as DefaultProject).evaluate()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `main class has incorrect value`(mainClass: String) {
        val project =
            ProjectBuilder.builder()
                .build()
        project.version = "1.0.0"
        project.pluginManager.apply(ComponentTh2Plugin::class.java)
        project.the<JavaApplication>().mainClass.set(mainClass)

        assertThrows<GradleException> {
            // We need to think how to reorganize plugins to avoid calling evaluation in tests
            (project as DefaultProject).evaluate()
        }
    }


}
