package com.exactpro.th2.gradle

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.property
import java.net.URI
import javax.inject.Inject

open class PublishTh2Extension @Inject constructor(
    objectFactory: ObjectFactory,
) {
    val pom: Pom = objectFactory.newInstance(Pom::class.java)

    val nexus: Nexus = objectFactory.newInstance(Nexus::class.java)

    val sonatype: Sonatype = objectFactory.newInstance(Sonatype::class.java)

    fun pom(block: Action<in Pom>) {
        block.execute(pom)
    }

    fun nexus(block: Action<in Nexus>) {
        block.execute(nexus)
    }

    fun sonatype(block: Action<in Sonatype>) {
        block.execute(sonatype)
    }
}

open class Pom @Inject constructor(
    objectFactory: ObjectFactory,
) {
    val vcsUrl: Provider<String> = objectFactory.property()
}

open class Nexus @Inject constructor(
    objectFactory: ObjectFactory,
) {
    val url: Provider<URI> = objectFactory.property()

    val username: Provider<String> = objectFactory.property()

    val password: Provider<String> = objectFactory.property()
}

open class Sonatype @Inject constructor(
    objectFactory: ObjectFactory,
) {
    val username: Provider<String> = objectFactory.property()

    val password: Provider<String> = objectFactory.property()
}