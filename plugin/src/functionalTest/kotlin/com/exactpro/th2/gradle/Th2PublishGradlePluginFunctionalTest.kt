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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains

class Th2PublishGradlePluginFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    @Test
    fun `all sonatype tasks present when configured via extension`() {
        // Set up the test build
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id('java')
                id('maven-publish')
                id('com.exactpro.th2.gradle.publish')
            }
            
            group = "com.example"
            version = "1.0.0"
            description = "test description"
            
            th2Publish {
              pom {
                vcsUrl.set("test")
              }
              sonatype {
                username.set("username")
                password.set("password")
              }
              signature {
                key.set("key")
                password.set("pwd")
              }
            }
            """.trimIndent(),
        )

        // Run the build
        val result =
            GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withConfiguredVersion()
                .withProjectDir(projectDir)
                .withArguments("--stacktrace", "tasks")
                .build()

        // Verify the result
        assertAll(
            {
                result.assertHasTask("publishToSonatype")
            },
            {
                result.assertHasTask("releaseSonatypeStagingRepository")
            },
            {
                result.assertHasTask("closeAndReleaseSonatypeStagingRepository")
            },
        )
    }

    @Test
    fun `all sonatype tasks present when configured via env variables`() {
        // Set up the test build
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id('java')
                id('maven-publish')
                id('com.exactpro.th2.gradle.publish')
            }
            
            group = "com.example"
            version = "1.0.0"
            description = "test description"
            """.trimIndent(),
        )

        // Run the build
        val result =
            GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withConfiguredVersion()
                .withProjectDir(projectDir)
                .withEnvironment(
                    mapOf(
                        "ORG_GRADLE_PROJECT_sonatypeUsername" to "user",
                        "ORG_GRADLE_PROJECT_sonatypePassword" to "pwd",
                        "ORG_GRADLE_PROJECT_signingKey" to "signKey",
                        "ORG_GRADLE_PROJECT_signingPassword" to "signPassword",
                    ),
                )
                .withArguments("--stacktrace", "tasks", "-Pvcs_url=test")
                .build()

        // Verify the result
        assertAll(
            {
                result.assertHasTask("publishToSonatype")
            },
            {
                result.assertHasTask("releaseSonatypeStagingRepository")
            },
            {
                result.assertHasTask("closeAndReleaseSonatypeStagingRepository")
            },
        )
    }

    @Test
    fun `reports error if version is missing`() {
        // Set up the test build
        settingsFile.writeText(
            """
            rootProject.name = "test"
            """.trimIndent(),
        )
        buildFile.writeText(
            """
            plugins {
                id('java')
                id('maven-publish')
                id('com.exactpro.th2.gradle.publish')
            }
            
            group = "com.example"
            // version = "1.0.0"
            description = "test description"
            th2Publish {
              pom {
                vcsUrl.set("test")
              }
            }
            """.trimIndent(),
        )

        val exception =
            assertThrows<UnexpectedBuildFailure> {
                GradleRunner.create()
                    .forwardOutput()
                    .withPluginClasspath()
                    .withConfiguredVersion()
                    .withProjectDir(projectDir)
                    .withEnvironment(
                        mapOf(
                            "ORG_GRADLE_PROJECT_sonatypeUsername" to "user",
                            "ORG_GRADLE_PROJECT_sonatypePassword" to "pwd",
                            "ORG_GRADLE_PROJECT_signingKey" to "signKey",
                            "ORG_GRADLE_PROJECT_signingPassword" to "signPassword",
                        ),
                    )
                    .withArguments("--stacktrace", "tasks")
                    .build()
            }

        assertContains(
            exception.buildResult.output,
            """
            project 'test' contains following issues:
            version is not provided (use version property)
            """.trimIndent(),
        )
    }

    @Test
    fun `reports error if group is empty`() {
        // Set up the test build
        settingsFile.writeText(
            """
            rootProject.name = "test"
            """.trimIndent(),
        )
        buildFile.writeText(
            """
            plugins {
                id('java')
                id('maven-publish')
                id('com.exactpro.th2.gradle.publish')
            }
            
            group = ""
            version = "1.0.0"
            description = "test description"
            th2Publish {
              pom {
                vcsUrl.set("test")
              }
            }
            """.trimIndent(),
        )

        val exception =
            assertThrows<UnexpectedBuildFailure> {
                GradleRunner.create()
                    .forwardOutput()
                    .withPluginClasspath()
                    .withConfiguredVersion()
                    .withProjectDir(projectDir)
                    .withEnvironment(
                        mapOf(
                            "ORG_GRADLE_PROJECT_sonatypeUsername" to "user",
                            "ORG_GRADLE_PROJECT_sonatypePassword" to "pwd",
                            "ORG_GRADLE_PROJECT_signingKey" to "signKey",
                            "ORG_GRADLE_PROJECT_signingPassword" to "signPassword",
                        ),
                    )
                    .withArguments("--stacktrace", "tasks")
                    .build()
            }

        assertContains(
            exception.buildResult.output,
            """
            project 'test' contains following issues:
            group is not provided (use group property)
            """.trimIndent(),
        )
    }

    @Test
    fun `reports error if descriptoin is missing`() {
        // Set up the test build
        settingsFile.writeText(
            """
            rootProject.name = "test"
            """.trimIndent(),
        )
        buildFile.writeText(
            """
            plugins {
                id('java')
                id('maven-publish')
                id('com.exactpro.th2.gradle.publish')
            }
            
            group = "com.example"
            version = "1.0.0"
            //description = "test description"
            th2Publish {
              pom {
                vcsUrl.set("test")
              }
            }
            """.trimIndent(),
        )

        val exception =
            assertThrows<UnexpectedBuildFailure> {
                GradleRunner.create()
                    .forwardOutput()
                    .withPluginClasspath()
                    .withConfiguredVersion()
                    .withProjectDir(projectDir)
                    .withEnvironment(
                        mapOf(
                            "ORG_GRADLE_PROJECT_sonatypeUsername" to "user",
                            "ORG_GRADLE_PROJECT_sonatypePassword" to "pwd",
                            "ORG_GRADLE_PROJECT_signingKey" to "signKey",
                            "ORG_GRADLE_PROJECT_signingPassword" to "signPassword",
                        ),
                    )
                    .withArguments("--stacktrace", "tasks")
                    .build()
            }

        assertContains(
            exception.buildResult.output,
            """
            project 'test' contains following issues:
            description is not provided (use description property)
            """.trimIndent(),
        )
    }

    @Test
    fun `reports error if vcs url is missing`() {
        // Set up the test build
        settingsFile.writeText(
            """
            rootProject.name = "test"
            """.trimIndent(),
        )
        buildFile.writeText(
            """
            plugins {
                id('java')
                id('maven-publish')
                id('com.exactpro.th2.gradle.publish')
            }
            
            group = "com.example"
            version = "1.0.0"
            description = "test description"
            """.trimIndent(),
        )

        val exception =
            assertThrows<UnexpectedBuildFailure> {
                GradleRunner.create()
                    .forwardOutput()
                    .withPluginClasspath()
                    .withConfiguredVersion()
                    .withProjectDir(projectDir)
                    .withEnvironment(
                        mapOf(
                            "ORG_GRADLE_PROJECT_sonatypeUsername" to "user",
                            "ORG_GRADLE_PROJECT_sonatypePassword" to "pwd",
                            "ORG_GRADLE_PROJECT_signingKey" to "signKey",
                            "ORG_GRADLE_PROJECT_signingPassword" to "signPassword",
                        ),
                    )
                    .withArguments("--stacktrace", "tasks")
                    .build()
            }

        assertContains(
            exception.buildResult.output,
            """
            project 'test' contains following issues:
            vcs url is not provided (use th2Publish.pom extension or vcs_url property)
            """.trimIndent(),
        )
    }

    private fun BuildResult.assertHasTask(taskName: String) {
        assertTrue(
            output.contains(taskName),
        ) {
            "not '$taskName' task found"
        }
    }
}
