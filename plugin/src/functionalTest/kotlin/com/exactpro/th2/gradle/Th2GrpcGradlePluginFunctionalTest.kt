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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

class Th2GrpcGradlePluginFunctionalTest {
    @field:TempDir
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
            th2Grpc {
              service.set(true)
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
    fun `proto is generated when applied to root project without service`() {
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
                    name = 'Sonatype_snapshots'
                    url = 'https://s01.oss.sonatype.org/content/repositories/snapshots/'
                }
                maven {
                    name 'Sonatype_releases'
                    url 'https://s01.oss.sonatype.org/content/repositories/releases/'
                }
            }
            """.trimIndent(),
        )
        projectDir.writeProtoFile(service = false)

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
            { assertAllSourcesGenerated(buildDirectory, service = false) },
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
                        name = 'Sonatype_snapshots'
                        url = 'https://s01.oss.sonatype.org/content/repositories/snapshots/'
                    }
                    maven {
                        name = 'Sonatype_releases'
                        url = 'https://s01.oss.sonatype.org/content/repositories/releases/'
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
            th2Grpc {
              service.set(true)
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

    private fun File.writeProtoFile(service: Boolean = true) {
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
            ${if (service) {
                """
                service Test {
                    rpc TestMethod(TestClass) returns (google.protobuf.Empty);
                }
                """.trimIndent()
            } else {
                ""
            }}
            
            message TestClass {}
            """.trimIndent(),
        )
    }

    private fun assertAllSourcesGenerated(
        buildDirectory: File,
        service: Boolean = true,
    ) {
        val generatedSources = buildDirectory / "generated" / "source" / "proto" / "main"
        val resources = buildDirectory / "resources" / "main"
        val metaInf = resources / "META-INF"
        assertAll(
            heading = "generated sources",
            {
                if (!service) {
                    return@assertAll
                }
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
                        grpc@{
                            if (!service) {
                                return@grpc
                            }
                            assertFileExist(resolve("TestOuterClass.java"))
                        },
                    )
                }
            },
            {
                if (!service) {
                    return@assertAll
                }
                with(generatedSources / "grpc" / "com" / "exactpro" / "th2" / "test" / "grpc") {
                    assertAll(
                        heading = "generated grpc",
                        { assertFileExist(resolve("TestGrpc.java")) },
                    )
                }
            },
            {
                if (!service) {
                    return@assertAll
                }
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
                if (!service) {
                    return@assertAll
                }
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
