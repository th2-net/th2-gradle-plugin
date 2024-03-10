package com.exactpro.th2.gradle

import com.palantir.gradle.docker.PalantirDockerPlugin
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaApplication
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComponentTh2PluginTest {
    @Test
    fun `applies all required plugins`() {
        val project =
            ProjectBuilder.builder()
                .build()
        project.pluginManager.apply(ComponentTh2Plugin::class.java)

        assertAll(
            {
                assertTrue(
                    project.plugins.hasPlugin(ApplicationPlugin::class.java),
                    "application plugin was not applied",
                )
            },
            {
                assertTrue(
                    project.plugins.hasPlugin(PalantirDockerPlugin::class.java),
                    "docker plugin was not applied",
                )
            },
            {
                assertTrue(
                    project.tasks.getByName("dockerPrepare").dependsOn
                        .contains(project.tasks.getByName("installDist")),
                    "installDist dependency was not added to docker task",
                )
            },
            {
                assertEquals(
                    "service",
                    project.the<JavaApplication>().applicationName,
                    "unexpected application name",
                )
            },
        )
    }
}
