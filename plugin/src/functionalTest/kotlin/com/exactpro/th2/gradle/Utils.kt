package com.exactpro.th2.gradle

import org.gradle.testkit.runner.GradleRunner
import java.io.File

internal operator fun File.div(path: String): File = resolve(path)

internal fun GradleRunner.withConfiguredVersion(): GradleRunner =
    apply {
        System.getProperty("dev.gradleplugins.defaultGradleVersion").also(this::withGradleVersion)
    }
