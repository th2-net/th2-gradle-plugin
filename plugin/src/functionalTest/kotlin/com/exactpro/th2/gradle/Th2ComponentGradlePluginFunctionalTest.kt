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

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals

class Th2ComponentGradlePluginFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    @Test
    fun `build component`() {
        settingsFile.writeText(
            """
            rootProject.name = "test"
            """.trimIndent(),
        )

        val extraFile = "extra.txt"

        buildFile.writeText(
            """
            plugins {
                id('application')
                id('com.exactpro.th2.gradle.component')
            }
            
            version = "1.0.0"
            
            repositories {
                mavenCentral()
                maven {
                    name = 'Sonatype_snapshots'
                    url = 'https://s01.oss.sonatype.org/content/repositories/snapshots/'
                }
                maven {
                    name = 'Sonatype_releases'
                    url = 'https://s01.oss.sonatype.org/content/repositories/releases/'
                }
            }
            
            application {
                mainClass.set('test.Main')
            }
            
            docker {
                copySpec.from("$extraFile")
            }
            """.trimIndent(),
        )

        projectDir.resolve(extraFile).writeText("Hello World!")

        val result =
            GradleRunner.create()
                .forwardOutput()
                .withDebug(true)
                .withConfiguredVersion()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments(
                    "--stacktrace",
                    "dockerPrep",
                    // because no git repository exist in test
                    "-x",
                    "generateGitProperties",
                )
                .build()

        val buildDirectory = projectDir / "build"
        val dockerDirectory = buildDirectory / "docker"

        assertAll(
            { assertEquals(TaskOutcome.SUCCESS, result.task(":dockerPrepare")?.outcome, "unexpected preparation result") },
            { assertFileExist(dockerDirectory / "service") },
            { assertFileExist(dockerDirectory / "service" / "bin") },
            { assertFileExist(dockerDirectory / "service" / "lib") },
            { assertFileExist(dockerDirectory / extraFile) },
        )
    }

    @Test
    fun `reports error if version is not provided`() {
        settingsFile.writeText(
            """
            rootProject.name = "test"
            """.trimIndent(),
        )
        buildFile.writeText(
            """
            plugins {
                id('application')
                id('com.exactpro.th2.gradle.component')
            }
            
            repositories {
                mavenCentral()
                maven {
                    name = 'Sonatype_snapshots'
                    url = 'https://s01.oss.sonatype.org/content/repositories/snapshots/'
                }
                maven {
                    name = 'Sonatype_releases'
                    url = 'https://s01.oss.sonatype.org/content/repositories/releases/'
                }
            }
            
            application {
                mainClass.set('test.Main')
            }
            """.trimIndent(),
        )

        val exception =
            assertThrows<UnexpectedBuildFailure> {
                GradleRunner.create()
                    .forwardOutput()
                    .withDebug(true)
                    .withConfiguredVersion()
                    .withPluginClasspath()
                    .withProjectDir(projectDir)
                    .withArguments(
                        "--stacktrace",
                        "dockerPrep",
                        // because no git repository exist in test
                        "-x",
                        "generateGitProperties",
                    )
                    .build()
            }

        assertContains(
            exception.buildResult.output,
            "project 'test' missing version (use version property to provide the version)",
            message = "unexpected error",
        )
    }
}
