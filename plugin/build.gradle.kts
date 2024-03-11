import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.build.config)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.gradle.functional.test)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    gradlePluginDevelopment()
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

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

functionalTest {
    testingStrategies.set(
        listOf(
            strategies.coverageForGradleVersion("7.6"),
            strategies.coverageForLatestGlobalAvailableVersion,
        ),
    )
}

gradlePlugin {
    website.set("https://github.com/th2-net/th2-gradle-plugin")
    vcsUrl.set("https://github.com/th2-net/th2-gradle-plugin.git")

    plugins.create("base") {
        id = "com.exactpro.th2.gradle.base"
        displayName = "Plugin for applying general project configuration and check"
        description = "Plugin helps you configure jar manifest and check dependencies for vulnerabilities"
        tags.set(listOf("th2", "base"))
        implementationClass = "com.exactpro.th2.gradle.BaseTh2Plugin"
    }

    plugins.create("grpc") {
        id = "com.exactpro.th2.gradle.grpc"
        displayName = "Plugin for building gRPC proto for th2 components"
        description =
            "Plugin helps you configure project to generate source code from proto files for using in th2 components"
        tags.set(listOf("th2", "gRPC"))
        implementationClass = "com.exactpro.th2.gradle.GrpcTh2Plugin"
    }

    plugins.create("publish") {
        id = "com.exactpro.th2.gradle.publish"
        displayName = "Plugin for publishing maven artifacts to sonatype"
        description = "Plugin helps you configure your project to publish maven artifacts to sonatype"
        tags.set(listOf("th2", "publish"))
        implementationClass = "com.exactpro.th2.gradle.PublishTh2Plugin"
    }

    plugins.create("component") {
        id = "com.exactpro.th2.gradle.component"
        displayName = "Plugin packaging th2 component"
        description = "Plugin helps to package th2 component and prepare it for building docker image"
        tags.set(listOf("th2", "docker"))
        implementationClass = "com.exactpro.th2.gradle.ComponentTh2Plugin"
    }
}

tasks.withType<Test> {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}

buildConfig {
    generateAtSync.set(false)
    packageName.set("com.exactpro.th2.gradle.config")
    className.set("Libraries")
    useKotlinOutput {
        internalVisibility = true
    }

    buildConfigField("TH2_BOM", libs.th2.bom.get().toString())
    buildConfigField("PROTOC", libs.protoc.get().toString())
    buildConfigField("GRPC_PLUGIN", libs.grpc.protoc.plugin.get().toString())
    buildConfigField("SERVICE_GENERATOR_PLUGIN", libs.grpc.service.generator.get().toString())
}

ktlint {
    debug.set(true)
    version.set(libs.versions.ktlint)
    reporters {
        reporter(ReporterType.HTML)
    }
    filter {
        exclude("**/com/exactpro/th2/gradle/config/Libraries.kt")
    }
}
