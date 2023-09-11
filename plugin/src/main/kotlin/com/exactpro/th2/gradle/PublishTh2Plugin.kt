package com.exactpro.th2.gradle

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import java.net.URI

class PublishTh2Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        check(project == project.rootProject) {
            "th2 publish plugin must be applied to the root project but was applied to ${project.path}"
        }
        val extension = project.extensions.create("th2Publish", PublishTh2Extension::class.java)
        configureNexusPublishPlugin(project, extension)
        configurePublishPlugins(project, extension)
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

    private fun configurePublishPlugins(rootProject: Project, extension: PublishTh2Extension) {
        rootProject.allprojects.forEach { subProj ->
            subProj.plugins.withType<MavenPublishPlugin> {
                // executed only if maven-publish is applied
                configurePublishingExtension(subProj, extension)
            }
            subProj.plugins.withType<JavaPlugin> {
                // executed only if java is applied
                subProj.extensions.getByType<JavaPluginExtension>().apply {
                    // in order to match sonatype requirements
                    withSourcesJar()
                    withJavadocJar()
                }
            }
        }
    }

    private fun configurePublishingExtension(project: Project, extension: PublishTh2Extension) {
        project.extensions.getByType<PublishingExtension>().apply {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(project.components["java"])

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

            project.afterEvaluate {
                val nexusRepoName = "nexus"
                repositories {
                    //Nexus repo to publish from gitlab
                    val nexus = extension.nexus
                    if (nexus.url.isPresent) {
                        maven {
                            name = nexusRepoName
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
}