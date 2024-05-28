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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertIs
import kotlin.test.assertNotNull

private val MAPPER = ObjectMapper()

class Th2BaseGradlePluginFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    @Test
    fun `licenses plugin finds license for kotlin multiplaform dependencies`() {
        settingsFile.writeText(
            """
            rootProject.name = "test"
            """.trimIndent(),
        )
        buildFile.writeText(
            """
            plugins {
                id('java-library')
                id('org.jetbrains.kotlin.jvm') version '1.8.22'
                id('com.exactpro.th2.gradle.base')
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation platform('io.ktor:ktor-bom:2.3.3')
                implementation 'io.ktor:ktor-server'
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .forwardOutput()
                .withDebug(true)
                .withConfiguredVersion()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments(
                    "--stacktrace",
                    "checkLicense",
                    // because no git repository exist in test
                    "-x",
                    "generateGitProperties",
                )
                .build()
        val checkLicenses = result.task(":checkLicense")
        assertNotNull(checkLicenses, "task checkLicense was not executed") {
            assertEquals(TaskOutcome.SUCCESS, it.outcome, "unexpected task result")
        }

        val licenses = MAPPER.readTree(projectDir.resolve("build/reports/dependency-license/licenses.json"))
        assertIs<ObjectNode>(licenses, "incorrect licenses file structure")
        val dependencies = assertIs<ArrayNode>(licenses.get("dependencies"), "dependencies must be a collection")

        assertAll(
            {
                dependencies.assertHasModuleLicenses("io.ktor:ktor-bom")
            },
            {
                dependencies.assertHasModuleLicenses("io.ktor:ktor-server-default-headers")
            },
        )
    }

    private fun ArrayNode.assertHasModuleLicenses(moduleName: String) {
        val module =
            elements().asSequence()
                .find { it.get("moduleName").textValue() == moduleName }
        assertNotNull(module, "module $moduleName not found") { m ->
            val moduleInfo = assertIs<ObjectNode>(m, "module info muse be an object")
            assertNotNull(moduleInfo.get("moduleLicenses"), "module $moduleName does not have licenses") {
                assertIs<ArrayNode>(it, "module $moduleName licenses must be a collection")
                assertFalse(it.isEmpty, "module $moduleName has empty licenses collection")
            }
        }
    }
}
