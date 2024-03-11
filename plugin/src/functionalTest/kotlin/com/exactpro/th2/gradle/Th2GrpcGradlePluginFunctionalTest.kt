package com.exactpro.th2.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

class Th2GrpcGradlePluginFunctionalTest {
    @field:TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var projectDir: File

    @Test
    fun `proto and grpc are generated when applied to root project`() {
        val buildFile = projectDir.resolve("build.gradle")
        val settingsFile = projectDir.resolve("settings.gradle")
        settingsFile.writeText("rootProject.name = \"test\"")
        buildFile.writeText(
            """
            plugins {
                id('java-library')
                id('com.exactpro.th2.gradle.grpc')
            }
            group = 'com.exactpro.th2'
            version = '0.0.1'
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
            """.trimIndent(),
        )
        projectDir.writeProtoFile()

        val result =
            GradleRunner.create()
                .forwardOutput()
                .withDebug(true)
                .withPluginClasspath()
                .withConfiguredVersion()
                .withProjectDir(projectDir)
                .withArguments(
                    "--stacktrace",
                    ":build",
                    // because no git repository exist in test
                    "-x",
                    ":generateGitProperties",
                )
                .build()

        val buildDirectory = projectDir / "build"

        assertAll(
            { assertEquals(TaskOutcome.SUCCESS, result.task(":generateProto")?.outcome, "generateProto executed") },
            { assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava")?.outcome, "compileJava executed") },
            { assertAllSourcesGenerated(buildDirectory) },
        )
    }

    @Test
    fun `proto and grpc are generated when applied to sub-project`() {
        val subProject = projectDir.resolve("grpc")
        val buildFile = projectDir.resolve("build.gradle")
        val subProjectBuildFile =
            subProject
                .also { it.mkdirs() }
                .resolve("build.gradle")
        val settingsFile = projectDir.resolve("settings.gradle")

        settingsFile.writeText(
            """
            rootProject.name = "test"
            include(":grpc")
            """.trimIndent(),
        )
        buildFile.writeText(
            """
            plugins {
                id('com.exactpro.th2.gradle.base')
            }
            allprojects {
                group = 'com.exactpro.th2'
                version = '0.0.1'
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
            }
            """.trimIndent(),
        )
        subProjectBuildFile.writeText(
            """
            plugins {
                id('java-library')
                id('com.exactpro.th2.gradle.grpc')
            }
            """.trimIndent(),
        )
        subProject.writeProtoFile()

        val result =
            GradleRunner.create()
                .forwardOutput()
                .withDebug(true)
                .withPluginClasspath()
                .withConfiguredVersion()
                .withProjectDir(projectDir)
                .withArguments(
                    "--stacktrace",
                    "build",
                    // because no git repository exist in test
                    "-x",
                    "generateGitProperties",
                )
                .build()

        val buildDirectory = subProject / "build"

        assertAll(
            { assertEquals(TaskOutcome.SUCCESS, result.task(":grpc:generateProto")?.outcome, "generateProto executed") },
            { assertEquals(TaskOutcome.SUCCESS, result.task(":grpc:compileJava")?.outcome, "compileJava executed") },
            { assertAllSourcesGenerated(buildDirectory) },
        )
    }

    private fun File.writeProtoFile() {
        val protoFile =
            resolve("src")
                .resolve("main")
                .resolve("proto")
                .also {
                    it.mkdirs()
                }
                .resolve("test.proto")

        protoFile.writeText(
            """
            syntax = "proto3";
            package test;
            import "google/protobuf/empty.proto";
            option java_multiple_files = true;
            option java_package = "com.exactpro.th2.test.grpc";
            service Test {
                rpc TestMethod(TestClass) returns (google.protobuf.Empty);
            }
            
            message TestClass {}
            """.trimIndent(),
        )
    }

    private fun Th2GrpcGradlePluginFunctionalTest.assertAllSourcesGenerated(buildDirectory: File) {
        val generatedSources = buildDirectory / "generated" / "source" / "proto" / "main"
        val resources = buildDirectory / "resources" / "main"
        val metaInf = resources / "META-INF"
        assertAll(
            heading = "generated sources",
            {
                assertAll(
                    heading = "resources",
                    {
                        assertFileExist(metaInf / "services" / "com.exactpro.th2.test.grpc.AsyncTestService")
                    },
                    {
                        assertFileExist(metaInf / "services" / "com.exactpro.th2.test.grpc.TestService")
                    },
                )
            },
            {
                with(generatedSources / "java" / "com" / "exactpro" / "th2" / "test" / "grpc") {
                    assertAll(
                        heading = "generated protobuf",
                        { assertFileExist(resolve("TestClass.java")) },
                        { assertFileExist(resolve("TestClassOrBuilder.java")) },
                        { assertFileExist(resolve("TestOuterClass.java")) },
                    )
                }
            },
            {
                with(generatedSources / "grpc" / "com" / "exactpro" / "th2" / "test" / "grpc") {
                    assertAll(
                        heading = "generated grpc",
                        { assertFileExist(resolve("TestGrpc.java")) },
                    )
                }
            },
            {
                with(generatedSources / "services" / "com" / "exactpro" / "th2" / "test" / "grpc") {
                    assertAll(
                        heading = "generated java services",
                        { assertFileExist(resolve("AsyncTestService.java")) },
                        { assertFileExist(resolve("TestService.java")) },
                        { assertFileExist(resolve("TestDefaultAsyncImpl.java")) },
                        { assertFileExist(resolve("TestDefaultBlockingImpl.java")) },
                    )
                }
            },
            {
                with(generatedSources / "services" / "python") {
                    assertAll(
                        heading = "generated python services",
                        { assertFileExist(resolve("test_service.py")) },
                    )
                }
            },
        )
    }
}
