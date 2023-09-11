plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "1.8.20"
    id("com.gradle.plugin-publish") version "1.2.0"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("io.github.gradle-nexus:publish-plugin:1.3.0")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.9.4")
    implementation("org.owasp:dependency-check-gradle:8.4.0")
    // Use the Kotlin JUnit 5 integration.
    testImplementation(gradleTestKit())
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    val base by plugins.creating {
        id = "com.exactpro.th2.gradle.base"
        implementationClass = "com.exactpro.th2.gradle.BaseTh2Plugin"
    }
    // Define the plugin
    val grpc by plugins.creating {
        id = "com.exactpro.th2.gradle.grpc"
        implementationClass = "com.exactpro.th2.gradle.GrpcTh2Plugin"
    }

    val publish by plugins.creating {
        id = "com.exactpro.th2.gradle.publish"
        implementationClass = "com.exactpro.th2.gradle.PublishTh2Plugin"
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

tasks.getByName("compileFunctionalTestKotlin").dependsOn(tasks.getByName("compileKotlin"))

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}
