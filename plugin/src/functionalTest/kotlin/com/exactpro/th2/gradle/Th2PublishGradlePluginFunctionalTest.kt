package com.exactpro.th2.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.io.TempDir
import java.io.File

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
            """.trimIndent()
        )

        // Run the build
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
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
            }
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
            """.trimIndent()
        )

        // Run the build
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withEnvironment(
                mapOf(
                    "ORG_GRADLE_PROJECT_sonatypeUsername" to "user",
                    "ORG_GRADLE_PROJECT_sonatypePassword" to "pwd",
                    "ORG_GRADLE_PROJECT_signingKey" to "signKey",
                    "ORG_GRADLE_PROJECT_signingPassword" to "signPassword",
                )
            )
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
            }
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
