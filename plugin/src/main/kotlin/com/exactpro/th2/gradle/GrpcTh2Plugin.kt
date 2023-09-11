package com.exactpro.th2.gradle

import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.ProtobufExtension
import com.google.protobuf.gradle.ProtobufPlugin
import com.google.protobuf.gradle.id
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

private const val TH2_GRPC_EXTENSION = "th2Grpc"

class GrpcTh2Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(BaseTh2Plugin::class.java)
        val protobufExtension = applyProtobufPlugin(project)
        configureJavaPlugin(project, protobufExtension)
    }

    private fun applyProtobufPlugin(project: Project): ProtobufExtension {
        val grpcTh2Extension = project.extensions.create<GrpcTh2Extension>(TH2_GRPC_EXTENSION)
        project.pluginManager.apply(ProtobufPlugin::class.java)
        val protobufExtension = project.extensions.getByType<ProtobufExtension>()
        configureProtobufExtension(protobufExtension, grpcTh2Extension)
        return protobufExtension
    }

    private fun configureJavaPlugin(
        project: Project,
        protobufExtension: ProtobufExtension,
    ) {
        project.plugins.withType<JavaPlugin> {
            addResources(project, protobufExtension)
            configureJavaTaskDependencies(project)
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

    private fun configureProtobufExtension(
        protobufExtension: ProtobufExtension,
        grpcTh2Extension: GrpcTh2Extension,
    ) {
        with(protobufExtension) {
            protoc {
                val version = grpcTh2Extension.protocVersion.convention("3.23.2")
                artifact = "$PROTOC_ARTIFACT:${version.get()}"
            }
            plugins {
                val hasServices = grpcTh2Extension.includeServices.convention(false).get()
                id("grpc") {
                    val version = grpcTh2Extension.grpcVersion.convention("1.56.0")
                    artifact = "$GRPC_ARTIFACT:${version.get()}"
                }
                if (hasServices) {
                    id("services") {
                        val version = grpcTh2Extension.serviceGeneratorVersion.convention("3.4.0").get()
                        artifact = "$SERVICES_GENERATOR_ARTIFACT:$version:all@jar"
                    }
                }

                generateProtoTasks {
                    ofSourceSet("main").forEach {
                        it.plugins {
                            id("grpc") {}
                            if (hasServices) {
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
    }

    companion object {
        private const val PROTOC_ARTIFACT = "com.google.protobuf:protoc"
        private const val GRPC_ARTIFACT = "io.grpc:protoc-gen-grpc-java"
        private const val SERVICES_GENERATOR_ARTIFACT = "com.exactpro.th2:grpc-service-generator"
    }
}