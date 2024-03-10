package com.exactpro.th2.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.io.TempDir
import java.io.File
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
            """.trimIndent()
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
                    name 'Sonatype_snapshots'
                    url 'https://s01.oss.sonatype.org/content/repositories/snapshots/'
                }
                maven {
                    name 'Sonatype_releases'
                    url 'https://s01.oss.sonatype.org/content/repositories/releases/'
                }
            }
            
            application {
                mainClass.set('test.Main')
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .forwardOutput()
            .withDebug(true)
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments(
                "--stacktrace",
                "dockerPrep",
                "-x",
                "generateGitProperties", // because no git repository exist in test
            )
            .build()

        val buildDirectory = projectDir / "build"
        val dockerDirectory = buildDirectory / "docker"

        assertAll(
            { assertEquals(TaskOutcome.SUCCESS, result.task(":dockerPrepare")?.outcome, "unexpected preparation result") },
            { assertFileExist(dockerDirectory / "service") },
        )
    }
}