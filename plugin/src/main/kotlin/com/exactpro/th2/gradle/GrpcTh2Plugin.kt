package com.exactpro.th2.gradle

import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.ProtobufExtension
import com.google.protobuf.gradle.ProtobufPlugin
import com.google.protobuf.gradle.id
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

class GrpcTh2Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin(BaseTh2Plugin::class.java)) {
            project.pluginManager.apply(BaseTh2Plugin::class.java)
        }
        project.pluginManager.apply(ProtobufPlugin::class.java)
        val protobufExtension = project.the<ProtobufExtension>()
        applyProtobufPlugin(protobufExtension)
        configureJavaPlugin(project, protobufExtension)
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
        }
        project.afterEvaluate {
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
        project.the<JavaPluginExtension>().sourceSets.apply {
            getByName("main") {
                resources {
                    srcDirs(
                        "${protobufExtension.generatedFilesBaseDir}/main/services/java/resources"
                    )
                }
            }
        }
    }

    private fun ProtobufExtension.configureProtobufExtension() {
        protoc {
            artifact = "$PROTOC_ARTIFACT:3.23.2"
        }
        plugins {
            id("grpc") {
                artifact = "$GRPC_ARTIFACT:1.56.0"
            }
            id("services") {
                artifact = "$SERVICES_GENERATOR_ARTIFACT:3.4.0:all@jar"
            }
        }
        generateProtoTasks {
            all().forEach {
                it.plugins {
                    id("grpc") {}
                    id("services") {
                        this.option("javaInterfacesPath=./java/src")
                        this.option("javaInterfacesImplPath=./java/src")
                        this.option("javaMetaInfPath=./java/resources")
                        this.option("pythonPath=./python")
                    }
                }
            }
//            ofSourceSet("main")
        }
    }

    companion object {
        private const val PROTOC_ARTIFACT = "com.google.protobuf:protoc"
        private const val GRPC_ARTIFACT = "io.grpc:protoc-gen-grpc-java"
        private const val SERVICES_GENERATOR_ARTIFACT = "com.exactpro.th2:grpc-service-generator"
    }
}