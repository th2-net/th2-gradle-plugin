package com.exactpro.th2.gradle

import com.palantir.gradle.docker.DockerExtension
import com.palantir.gradle.docker.PalantirDockerPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaApplication
import org.gradle.kotlin.dsl.the

class ComponentTh2Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            if (this === rootProject && !plugins.hasPlugin(BaseTh2Plugin::class.java)) {
                pluginManager.apply(BaseTh2Plugin::class.java)
            }
            if (!plugins.hasPlugin(ApplicationPlugin::class.java)) {
                pluginManager.apply(ApplicationPlugin::class.java)
            }
            the<JavaApplication>().applicationName = "service"
            pluginManager.apply(PalantirDockerPlugin::class.java)
            the<DockerExtension>().apply {
                copySpec.from("$buildDir/install")
            }
            tasks.getByName("dockerPrepare")
                .dependsOn(tasks.getByName(DistributionPlugin.TASK_INSTALL_NAME))
        }
    }
}
