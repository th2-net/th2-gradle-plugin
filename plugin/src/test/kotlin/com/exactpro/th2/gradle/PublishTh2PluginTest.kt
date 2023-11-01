package com.exactpro.th2.gradle

import org.gradle.kotlin.dsl.findByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

internal class PublishTh2PluginTest {
    @Test
    fun `applies required plugins`() {
        val project = ProjectBuilder.builder()
            .build()

        project.pluginManager.apply("com.exactpro.th2.gradle.publish")

        project.extensions.findByType<PublishTh2Extension>().apply {
            assertNotNull(this, "no publish th2 extension applied")
        }
    }
}