package com.exactpro.th2.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByName

private const val JAVA_VERSION_PROP = "java.version"

private const val JAVA_VENDOR_PROP = "java.vendor"

private const val EXACTPRO_SYSTEMS_LLC = "Exactpro Systems LLC"

private const val VENDOR_ID = "com.exactpro"

class BaseTh2Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.afterEvaluate {
            configureManifestInfo()
        }
    }

    private fun Project.configureManifestInfo() {
        tasks.getByName<Jar>(JavaPlugin.JAR_TASK_NAME) {
            manifest {
                val javaInfo = "${System.getProperty(JAVA_VERSION_PROP)} (${System.getProperty(JAVA_VENDOR_PROP)})"
                attributes(
                    mapOf(
                        "Created-By" to javaInfo,
                        "Specification-Title" to "",
                        "Specification-Vendor" to EXACTPRO_SYSTEMS_LLC,
                        "Implementation-Title" to this@configureManifestInfo.name,
                        "Implementation-Vendor" to EXACTPRO_SYSTEMS_LLC,
                        "Implementation-Vendor-Id" to VENDOR_ID,
                        "Implementation-Version" to this@configureManifestInfo.version
                    )
                )
            }
        }
    }
}