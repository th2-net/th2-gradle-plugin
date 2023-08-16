package com.exactpro.th2.gradle

import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.ProtobufExtension
import com.google.protobuf.gradle.ProtobufPlugin
import com.google.protobuf.gradle.id
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType

class GrpcTh2Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(BaseTh2Plugin::class.java)
        val protobufExtension = applyProtobufPlugin(project)
        applyJavaPlugin(project, protobufExtension)
    }

    private fun applyProtobufPlugin(project: Project): ProtobufExtension {
        project.pluginManager.apply(ProtobufPlugin::class.java)
        val protobufExtension = project.extensions.getByType<ProtobufExtension>()
        configureProtobufExtension(protobufExtension)
        return protobufExtension
    }

    private fun applyJavaPlugin(
        project: Project,
        protobufExtension: ProtobufExtension,
    ) {
        project.pluginManager.apply(JavaLibraryPlugin::class.java)
        addResources(project, protobufExtension)
        configureJavaTaskDependencies(project)
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
        project.extensions.getByType<JavaPluginExtension>().sourceSets.apply {
            getByName("main") {
                resources {
                    srcDirs(
                        "${protobufExtension.generatedFilesBaseDir}/main/services/java/resources"
                    )
                }
            }
        }
    }

    private fun configureProtobufExtension(protobufExtension: ProtobufExtension) {
        with(protobufExtension) {
            // TODO: make versions configurable
            protoc {
                artifact = "$PROTOC_ARTIFACT:3.23.2"
            }
            plugins {
                id("grpc") {
                    artifact = "$GRPC_ARTIFACT:1.56.0"
                }
                // TODO: make service block optional (for grpc projects without services)
                id("services") {
                    artifact = "$SERVICES_GENERATOR_ARTIFACT:3.4.0:all@jar"
                }

                generateProtoTasks {
                    ofSourceSet("main").forEach {
                        it.plugins {
                            id("grpc") {}
                            id("services") {
                                option("javaInterfacesPath=./java/src")
                                option("javaInterfacesImplPath=./java/src")
                                option("javaMetaInfPath=./java/resources")
                                option("pythonPath=./python")
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val PROTOC_ARTIFACT = "com.google.protobuf:protoc"
        private const val GRPC_ARTIFACT = "io.grpc:protoc-gen-grpc-java"
        private const val SERVICES_GENERATOR_ARTIFACT = "com.exactpro.th2:grpc-service-generator"
    }
}