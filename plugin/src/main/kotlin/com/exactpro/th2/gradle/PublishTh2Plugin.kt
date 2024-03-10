package com.exactpro.th2.gradle

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.plugins.JavaPlatformPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import java.net.URI

internal const val TH2_PUBLISH_EXTENSION = "th2Publish"

private const val PUBLISHING_EXTENSION = "mavenJava"

private const val SONATYPE_RELEASE_URL = "https://s01.oss.sonatype.org/service/local/"

private const val SONATYPE_SNAPSHOT_URL = "https://s01.oss.sonatype.org/content/repositories/snapshots/"

private const val JAVA_PLATFORM_COMPONENT_NAME = "javaPlatform"

private const val JAVA_COMPONENT_NAME = JvmConstants.JAVA_COMPONENT_NAME

class PublishTh2Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin(BaseTh2Plugin::class.java)) {
            project.pluginManager.apply(BaseTh2Plugin::class.java)
        }
        check(project == project.rootProject) {
            "th2 publish plugin must be applied to the root project but was applied to ${project.path}"
        }
        val extension = project.extensions.create(TH2_PUBLISH_EXTENSION, PublishTh2Extension::class.java)

        configureSonatypeNexusPublishPlugin(project, extension)
        configurePublishPlugins(project, extension)
        configureSingingPlugin(project, extension)
    }

    private fun configureSonatypeNexusPublishPlugin(project: Project, extension: PublishTh2Extension) {
        project.afterEvaluate {
            // This causes the following:
            // If username and password neither configured via gradle.build nor properties
            // the nexus plugin is not applied at all.
            // This is quite questionable and probably should be changed to
            // enabling or disabling tasks depending on configuration provided
            if (!extension.sonatype.run { username.isPresent && password.isPresent }) {
                return@afterEvaluate
            }
            project.pluginManager.apply(NexusPublishPlugin::class.java)
            project.the<NexusPublishExtension>().apply {
                repositories {
                    sonatype {
                        nexusUrl.set(URI(SONATYPE_RELEASE_URL))
                        snapshotRepositoryUrl.set(URI(SONATYPE_SNAPSHOT_URL))
                        username.set(extension.sonatype.username)
                        password.set(extension.sonatype.password)
                    }
                }
            }
        }
    }

    private fun configureSingingPlugin(project: Project, extension: PublishTh2Extension) {
        project.rootProject.allprojects.forEach { subProj ->
            project.afterEvaluate {
                if (!extension.signature.run { key.isPresent && password.isPresent }) {
                    return@afterEvaluate
                }

                subProj.plugins.withType<MavenPublishPlugin> {
                    if (!subProj.plugins.hasPlugin(SigningPlugin::class.java)) {
                        subProj.pluginManager.apply(SigningPlugin::class.java)
                    }
                    configureSigningExtension(subProj, extension)
                }
            }
        }
    }

    private fun configurePublishPlugins(project: Project, extension: PublishTh2Extension) {
        project.rootProject.allprojects.forEach { subProj ->
            subProj.afterEvaluate {
                plugins.withType<MavenPublishPlugin> {
                    configurePublishingExtension(subProj, extension)
                    plugins.withType<JavaPlugin> {
                        the<JavaPluginExtension>().apply {
                            // in order to match sonatype requirements
                            withSourcesJar()
                            withJavadocJar()
                        }
                    }
                }
            }
        }
    }

    private fun configurePublishingExtension(project: Project, extension: PublishTh2Extension) {
        project.the<PublishingExtension>().apply {

            publications {
                create<MavenPublication>(PUBLISHING_EXTENSION) {
                    project.plugins.withType<JavaPlugin> {
                        from(project.components[JAVA_COMPONENT_NAME])
                    }
                    project.plugins.withType<JavaPlatformPlugin> {
                        from(project.components[JAVA_PLATFORM_COMPONENT_NAME])
                    }

                    pom {
                        name.set(project.provider { project.name })
                        packaging = "jar"
                        description.set(project.provider { project.description })
                        url.set(extension.pom.vcsUrl)

                        scm {
                            url.set(extension.pom.vcsUrl)
                        }

                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }

                        developers {
                            developer {
                                id.set("developer")
                                name.set("developer")
                                email.set("developer@exactpro.com")
                            }
                        }
                    }
                }
            }

            repositories {
                //Nexus repo to publish from gitlab
                val nexus = extension.nexus
                if (nexus.url.isPresent) {
                    maven {
                        name = "nexus"
                        url = nexus.url.get()

                        credentials {
                            username = nexus.username.orNull ?: error("username for nexus is not provided")
                            password = nexus.password.orNull ?: error("password for nexus is not provided")
                        }
                    }
                }
            }
        }
    }

    /**
     * Must be called after evaluation
     */
    private fun configureSigningExtension(project: Project, extension: PublishTh2Extension) {
        project.the<SigningExtension>().apply {
            useInMemoryPgpKeys(extension.signature.key.get(), extension.signature.password.get())
            sign(project.the<PublishingExtension>().publications.getByName(PUBLISHING_EXTENSION))
        }
    }
}