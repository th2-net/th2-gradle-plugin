package com.exactpro.th2.gradle

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlatformPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import java.net.URI

internal const val TH2_PUBLISH_EXTENSION = "th2Publish"

private const val PUBLISHING_EXTENSION = "mavenJava"

class PublishTh2Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        check(project == project.rootProject) {
            "th2 publish plugin must be applied to the root project but was applied to ${project.path}"
        }
        val extension = project.extensions.create(TH2_PUBLISH_EXTENSION, PublishTh2Extension::class.java)

        configureNexusPublishPlugin(project, extension)
        configurePublishPlugins(project, extension)
        configureSingningPlugin(project, extension)
    }

    private fun configureNexusPublishPlugin(project: Project, extension: PublishTh2Extension) {
        project.afterEvaluate {
            if (!extension.sonatype.run { username.isPresent && password.isPresent }) {
                return@afterEvaluate
            }
            project.pluginManager.apply(NexusPublishPlugin::class.java)
            project.extensions.getByType<NexusPublishExtension>().apply {
                repositories {
                    sonatype {
                        nexusUrl.set(URI("https://s01.oss.sonatype.org/service/local/"))
                        snapshotRepositoryUrl.set(URI("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                    }
                }
            }
        }
    }

    private fun configureSingningPlugin(project: Project, extension: PublishTh2Extension) {
        project.rootProject.allprojects.forEach { subProj ->
            project.afterEvaluate {
                if (!extension.signature.run { key.isPresent && password.isPresent }) {
                    return@afterEvaluate
                }

                subProj.pluginManager.apply(SigningPlugin::class.java)
                configureSigningExtension(project, extension)
            }
        }
    }

    private fun configurePublishPlugins(project: Project, extension: PublishTh2Extension) {
        project.rootProject.allprojects.forEach { subProj ->
            subProj.afterEvaluate {
                subProj.pluginManager.apply(MavenPublishPlugin::class.java)
                configurePublishingExtension(subProj, extension)
            }

            if (subProj.plugins.hasPlugin(JavaPlatformPlugin::class.java)) {
                return@forEach
            }

            subProj.pluginManager.apply(JavaPlugin::class.java)
            subProj.extensions.getByType<JavaPluginExtension>().apply {
                // in order to match sonatype requirements
                withSourcesJar()
                withJavadocJar()
            }
        }
    }

    private fun configurePublishingExtension(project: Project, extension: PublishTh2Extension) {
        project.afterEvaluate {
            project.extensions.getByType<PublishingExtension>().apply {

                publications {
                    create<MavenPublication>(PUBLISHING_EXTENSION) {
                        project.plugins.withType<JavaPlugin> {
                            from(project.components["java"])
                        }
                        project.plugins.withType<JavaPlatformPlugin> {
                            from(project.components["javaPlatform"])
                        }

                        pom {
                            name.set(project.name)
                            packaging = "jar"
                            description.set(project.description)
                            url.set(extension.pom.vcsUrl)

                            scm {
                                url.set(extension.pom.vcsUrl)
                            }

                            licenses {
                                license {
                                    name.set("The Apache License, Version 2.0")
                                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
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
    }

    private fun configureSigningExtension(project: Project, extension: PublishTh2Extension) {
        project.afterEvaluate {
            project.extensions.getByType<SigningExtension>().apply {
                useInMemoryPgpKeys(extension.signature.key.get(), extension.signature.password.get())
                sign(project.extensions.getByType<PublishingExtension>().publications.getByName(PUBLISHING_EXTENSION))
            }
        }
    }
}