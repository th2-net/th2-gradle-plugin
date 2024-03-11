/*
 * Copyright 2024 Exactpro (Exactpro Systems Limited)
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

import com.github.jk1.license.LicenseReportPlugin
import com.gorylenko.GitPropertiesPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.owasp.dependencycheck.gradle.DependencyCheckPlugin
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class BaseTh2PluginTest {
    @ParameterizedTest(name = "when {0} plugin applied")
    @ValueSource(strings = ["java", "java-library", "org.jetbrains.kotlin.jvm"])
    fun `applies required plugins`(javaPlugin: String) {
        val project =
            ProjectBuilder.builder()
                .build()

        project.pluginManager.apply(javaPlugin)
        project.pluginManager.apply("com.exactpro.th2.gradle.base")
        assertAll(
            {
                assertTrue(
                    project.plugins.hasPlugin(DependencyCheckPlugin::class.java),
                    "no dependencies check plugin applied",
                )
            },
            {
                assertTrue(
                    project.plugins.hasPlugin(LicenseReportPlugin::class.java),
                    "no license plugin applied",
                )
            },
            {
                assertTrue(
                    project.plugins.hasPlugin(GitPropertiesPlugin::class.java),
                    "no git properties plugin applied",
                )
            },
            {
                val bom =
                    assertNotNull(
                        project.configurations.findByName("implementation"),
                        "no implementation configuration found",
                    ).allDependencies
                        .find {
                            it.group == "com.exactpro.th2" &&
                                it.name == "bom"
                        }
                assertNotNull(bom, "bom not found")
            },
        )
    }

    @Test
    fun `reports error if applied not to the root project`() {
        val root =
            ProjectBuilder.builder()
                .build()
        val subProject =
            ProjectBuilder.builder()
                .withParent(root)
                .build()

        assertThrows<Exception> {
            subProject.pluginManager.apply(BaseTh2Plugin::class.java)
        }
    }
}
