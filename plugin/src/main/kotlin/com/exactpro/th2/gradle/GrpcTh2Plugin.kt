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

package com.exactpro.th2.gradle

import com.exactpro.th2.gradle.config.Libraries
import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.ProtobufExtension
import com.google.protobuf.gradle.ProtobufPlugin
import com.google.protobuf.gradle.id
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

class GrpcTh2Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project === project.rootProject) {
            if (!project.plugins.hasPlugin(BaseTh2Plugin::class.java)) {
                project.pluginManager.apply(BaseTh2Plugin::class.java)
            }
        }
        checkJavaLibraryPlugin(project)
        project.pluginManager.apply(ProtobufPlugin::class.java)
        val protobufExtension = project.the<ProtobufExtension>()
        applyProtobufPlugin(protobufExtension)
        configureJavaPlugin(project, protobufExtension)
    }

    private fun checkJavaLibraryPlugin(project: Project) {
        if (!project.plugins.hasPlugin(JavaLibraryPlugin::class.java)) {
            if (project.plugins.hasPlugin(JavaPlugin::class.java)) {
                error("java-library plugin must be applied to the project with gRPC but a different one was")
            }
            project.pluginManager.apply(JavaLibraryPlugin::class.java)
        }
    }

    private fun applyProtobufPlugin(protobufExtension: ProtobufExtension) {
        protobufExtension.configureProtobufExtension()
    }

    private fun configureJavaPlugin(
        project: Project,
        protobufExtension: ProtobufExtension,
    ) {
        project.plugins.withType<JavaPlugin> {
            addResources(project, protobufExtension)
            configureJavaDependencies(project)
            project.afterEvaluate {
                configureJavaTaskDependencies(project)
            }
        }
    }

    private fun DependencyHandler.api(notation: Any) {
        add(JavaPlugin.API_CONFIGURATION_NAME, notation)
    }

    private fun DependencyHandler.impl(notation: Any) {
        add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, notation)
    }

    private fun configureJavaDependencies(project: Project) {
        project.dependencies.apply {
            api("com.google.protobuf:protobuf-java-util")
            api("io.grpc:grpc-stub")

            impl("io.grpc:grpc-protobuf")
            impl("io.grpc:grpc-core")
            impl("io.grpc:grpc-netty")
            impl("javax.annotation:javax.annotation-api:1.3.2")
            impl(Libraries.SERVICE_GENERATOR_PLUGIN)
        }
    }

    private fun configureJavaTaskDependencies(project: Project) {
        val generateProtoTask = project.tasks.getByName<GenerateProtoTask>("generateProto")

        fun addDependency(taskName: String) {
            project.tasks.findByName(taskName)?.dependsOn(generateProtoTask)
        }
        listOf(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaPlugin.PROCESS_RESOURCES_TASK_NAME)
            .forEach { addDependency(it) }
    }

    private fun addResources(
        project: Project,
        protobufExtension: ProtobufExtension,
    ) {
        project.the<JavaPluginExtension>().sourceSets.apply {
            getByName("main") {
                resources {
                    srcDirs(
                        "${protobufExtension.generatedFilesBaseDir}/main/services/resources",
                    )
                }
            }
        }
    }

    private fun ProtobufExtension.configureProtobufExtension() {
        protoc {
            artifact = Libraries.PROTOC
        }
        plugins {
            id("grpc") {
                artifact = Libraries.GRPC_PLUGIN
            }
            id("services") {
                artifact = "${Libraries.SERVICE_GENERATOR_PLUGIN}:all@jar"
            }
        }
        generateProtoTasks {
            all().forEach {
                it.plugins {
                    id("grpc") {}
                    id("services") {
                        this.option("javaInterfacesPath=.")
                        this.option("javaInterfacesImplPath=.")
                        this.option("javaMetaInfPath=./resources")
                        this.option("pythonPath=./python")
                    }
                }
            }
            ofSourceSet("main")
        }
    }
}
