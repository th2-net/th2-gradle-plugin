plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradle.publish)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.nexus.publish)
    implementation(libs.protobuf)
    implementation(libs.licenses)
    implementation(libs.download)
    implementation(libs.git.properties)

    implementation(libs.owasp)

    implementation(libs.docker)
    // Use the Kotlin JUnit 5 integration.
    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.parameters)
    testImplementation(libs.junit.kotlin)
    testImplementation(libs.kotlin.plugin)
    testRuntimeOnly(libs.junit.launcher)
}

// Add a source set for the functional test suite
val functionalTestSourceSet: SourceSet = sourceSets.create("functionalTest") { }

tasks.getByName("compileFunctionalTestKotlin").dependsOn(tasks.getByName("compileKotlin"))

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}


gradlePlugin {
    website.set("https://github.com/th2-net/th2-gradle-plugin")
    vcsUrl.set("https://github.com/th2-net/th2-gradle-plugin.git")

    val base by plugins.creating {
        id = "com.exactpro.th2.gradle.base"
        displayName = "Plugin for applying general project configuration and check"
        description = "Plugin helps you configure jar manifest and check dependencies for vulnerabilities"
        tags.set(listOf("th2", "base"))
        implementationClass = "com.exactpro.th2.gradle.BaseTh2Plugin"
    }
    // Define the plugin
    val grpc by plugins.creating {
        id = "com.exactpro.th2.gradle.grpc"
        displayName = "Plugin for building gRPC proto for th2 components"
        description = "Plugin helps you configure project to generate source code from proto files for using in th2 components"
        tags.set(listOf("th2", "gRPC"))
        implementationClass = "com.exactpro.th2.gradle.GrpcTh2Plugin"
    }

    val publish by plugins.creating {
        id = "com.exactpro.th2.gradle.publish"
        displayName = "Plugin for publishing maven artifacts to sonatype"
        description = "Plugin helps you configure your project to publish maven artifacts to sonatype"
        tags.set(listOf("th2", "publish"))
        implementationClass = "com.exactpro.th2.gradle.PublishTh2Plugin"
    }

    val component by plugins.creating {
        id = "com.exactpro.th2.gradle.component"
        displayName = "Plugin packaging th2 component"
        description = "Plugin helps to package th2 component and prepare it for building docker image"
        tags.set(listOf("th2", "docker"))
        implementationClass = "com.exactpro.th2.gradle.ComponentTh2Plugin"
    }

    testSourceSets.add(functionalTestSourceSet)
}

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}
