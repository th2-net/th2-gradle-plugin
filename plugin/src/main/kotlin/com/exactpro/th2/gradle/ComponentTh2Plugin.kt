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

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.register
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
            val dockerExtension = extensions.create("docker", DockerTh2Extension::class.java)
            dockerExtension.copySpec.from(project.layout.buildDirectory.dir("install"))

            val dockerDir = project.layout.buildDirectory.dir("docker")

            val dockerClean =
                tasks.register<Delete>("dockerClean") {
                    delete(dockerDir)
                }

            tasks.register<Copy>("dockerPrepare") {
                dependsOn(tasks.getByName(DistributionPlugin.TASK_INSTALL_NAME), dockerClean)

                with(dockerExtension.copySpec)

                into(dockerDir)
            }

            afterEvaluate {
                if (version.toString().let { it == Project.DEFAULT_VERSION || it.isEmpty() }) {
                    throw GradleException("project '$name' missing version (use version property to provide the version)")
                }
            }
        }
    }
}
