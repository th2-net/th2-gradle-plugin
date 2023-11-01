package com.exactpro.th2.gradle

import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.owasp.dependencycheck.gradle.DependencyCheckPlugin
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class BaseTh2PluginTest {
    @Test
    fun `applies required plugins`() {
        val project = ProjectBuilder.builder()
            .build()

        project.pluginManager.apply("com.exactpro.th2.gradle.base")

        assertTrue(
            project.plugins.hasPlugin(JavaPlugin::class.java),
            "no java plugin applied",
        )
        assertTrue(
            project.plugins.hasPlugin(DependencyCheckPlugin::class.java),
            "no dependencies check plugin applied",
        )
    }
}