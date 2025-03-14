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
import java.io.File

internal operator fun File.div(path: String): File = resolve(path)

internal fun GradleRunner.withConfiguredVersion(): GradleRunner =
    apply {
        System.getProperty("dev.gradleplugins.defaultGradleVersion").also(this::withGradleVersion)
    }

internal fun runBuild(projectDir: File): BuildResult =
    GradleRunner
        .create()
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
        ).build()
