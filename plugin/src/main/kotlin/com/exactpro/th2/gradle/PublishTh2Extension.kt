package com.exactpro.th2.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import java.net.URI
import javax.inject.Inject

open class PublishTh2Extension
    @Inject
    constructor(
        objectFactory: ObjectFactory,
    ) {
        val pom: Pom = objectFactory.newInstance(Pom::class.java)

        val nexus: Nexus = objectFactory.newInstance(Nexus::class.java)

        val sonatype: Sonatype = objectFactory.newInstance(Sonatype::class.java)

        val signature: Signature = objectFactory.newInstance(Signature::class.java)

        fun pom(block: Action<in Pom>) {
            block.execute(pom)
        }

        fun nexus(block: Action<in Nexus>) {
            block.execute(nexus)
        }

        fun sonatype(block: Action<in Sonatype>) {
            block.execute(sonatype)
        }

        fun signature(block: Action<in Signature>) {
            block.execute(signature)
        }
    }

open class Pom
    @Inject
    constructor(
        project: Project,
    ) {
        val vcsUrl: Property<String> =
            project.objects.property<String>().apply {
                set(
                    project.provider { project.findProperty("vcs_url") as? String },
                )
            }
    }

open class Nexus
    @Inject
    constructor(
        project: Project,
    ) {
        val url: Property<URI> =
            project.objects.property<URI>().apply {
                set(
                    project.provider {
                        (project.findProperty("nexus_url") as? String)?.let(URI::create)
                    },
                )
            }

        val username: Property<String> =
            project.objects.property<String>().apply {
                set(project.provider { project.findProperty("nexus_user") as? String })
            }

        val password: Property<String> =
            project.objects.property<String>().apply {
                set(project.provider { project.findProperty("nexus_password") as? String })
            }
    }

open class Sonatype
    @Inject
    constructor(
        project: Project,
    ) {
        val username: Property<String> =
            project.objects.property<String>().apply {
                set(project.provider { project.findProperty("sonatypeUsername") as? String })
            }

        val password: Property<String> =
            project.objects.property<String>().apply {
                set(project.provider { project.findProperty("sonatypePassword") as? String })
            }
    }

open class Signature
    @Inject
    constructor(
        project: Project,
    ) {
        val key: Property<String> =
            project.objects.property<String>().apply {
                set(
                    project.provider { project.findProperty("signingKey") as? String },
                )
            }

        val password: Property<String> =
            project.objects.property<String>().apply {
                set(
                    project.provider { project.findProperty("signingPassword") as? String },
                )
            }
    }
