package com.exactpro.th2.gradle

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import java.net.URI
import javax.inject.Inject

open class PublishTh2Extension @Inject constructor(
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

open class Pom @Inject constructor(
    objectFactory: ObjectFactory,
) {
    val vcsUrl: Property<String> = objectFactory.property()
}

open class Nexus @Inject constructor(
    objectFactory: ObjectFactory,
) {
    val url: Property<URI> = objectFactory.property()

    val username: Property<String> = objectFactory.property()

    val password: Property<String> = objectFactory.property()
}

open class Sonatype @Inject constructor(
    objectFactory: ObjectFactory,
) {
    val username: Property<String> = objectFactory.property()

    val password: Property<String> = objectFactory.property()
}
open class Signature @Inject constructor(
    objectFactory: ObjectFactory,
) {
    val key: Property<String> = objectFactory.property()

    val password: Property<String> = objectFactory.property()
}