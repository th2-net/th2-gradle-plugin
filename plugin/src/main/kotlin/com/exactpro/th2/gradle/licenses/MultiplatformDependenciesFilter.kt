/*
 * Copyright 2024 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.gradle.licenses

import com.github.jk1.license.ModuleData
import com.github.jk1.license.ProjectData
import com.github.jk1.license.filter.DependencyFilter
import org.gradle.api.logging.Logging

/**
 * Class attempts to find metadata dependencies for multiplatform libraries
 * and update their licenses data based on the resolved 'twin'
 *
 * For examples,
 *
 * If `io.ktor:ktor-server:2.3.3` dependency is specified for JVM project
 * the actual dependency will be `io.ktor:ktor-server-jvm:2.3.3`.
 *
 * Because of that we can find all the dependencies that does not have licenses metadata
 * and have a dependency with `-jvm` suffix in module name.
 *
 *
 * The reason this filter is required because some of the 'aggregated' dependencies does not result into an artifact.
 * Because of that the licenses plugin cannot find any metadata for them and produces empty [ModuleData] object.
 */
internal object MultiplatformDependenciesFilter : DependencyFilter {
    override fun filter(source: ProjectData): ProjectData {
        source.configurations.forEach { configuration ->
            LOGGER.info("Processing configuration {}", configuration.name)
            configuration.dependencies
                .asSequence()
                .filter(ModuleData::isEmpty)
                .forEach moduleForEach@{ module ->
                    val expectedModuleName = "${module.name}-jvm"
                    val twinModule =
                        configuration.dependencies.find {
                            it.group == module.group && it.name == expectedModuleName && it.version == module.version
                        } ?: return@moduleForEach
                    LOGGER.info(
                        "Found 'twin' dependency {}:{}:{} for {}. Updating licenses data",
                        twinModule.group,
                        twinModule.name,
                        twinModule.version,
                        module,
                    )
                    module.poms.addAll(twinModule.poms)
                    module.licenseFiles.addAll(twinModule.licenseFiles)
                    module.manifests.addAll(twinModule.manifests)
                }
        }
        return source
    }

    private val LOGGER = Logging.getLogger(MultiplatformDependenciesFilter::class.java)
}
