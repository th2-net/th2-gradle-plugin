import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    signing
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
            strategies.coverageForGradleVersion(GradleVersion.current().version),
        ),
    )
}

@Suppress("PropertyName")
val vcs_url: String by project

gradlePlugin {
    website.set("https://github.com/th2-net/th2-gradle-plugin")
    vcsUrl.set(vcs_url)

    plugins {
        create("base") {
            id = "com.exactpro.th2.gradle.base"
            displayName = "Plugin for applying general project configuration and check"
            description = "Plugin helps you configure jar manifest and check dependencies for vulnerabilities"
            tags.set(listOf("th2", "base"))
            implementationClass = "com.exactpro.th2.gradle.BaseTh2Plugin"
        }

        create("grpc") {
            id = "com.exactpro.th2.gradle.grpc"
            displayName = "Plugin for building gRPC proto for th2 components"
            description =
                "Plugin helps you configure project to generate source code from proto files for using in th2 components"
            tags.set(listOf("th2", "gRPC"))
            implementationClass = "com.exactpro.th2.gradle.GrpcTh2Plugin"
        }

        create("publish") {
            id = "com.exactpro.th2.gradle.publish"
            displayName = "Plugin for publishing maven artifacts to sonatype"
            description = "Plugin helps you configure your project to publish maven artifacts to sonatype"
            tags.set(listOf("th2", "publish"))
            implementationClass = "com.exactpro.th2.gradle.PublishTh2Plugin"
        }

        create("component") {
            id = "com.exactpro.th2.gradle.component"
            displayName = "Plugin packaging th2 component"
            description = "Plugin helps to package th2 component and prepare it for building docker image"
            tags.set(listOf("th2", "docker"))
            implementationClass = "com.exactpro.th2.gradle.ComponentTh2Plugin"
        }
    }
}

val publishPlugins by tasks.named("publishPlugins")

tasks.withType<Test> {
    publishPlugins.mustRunAfter(this)
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

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set(rootProject.name)
                packaging = "jar"
                description.set(rootProject.description)
                url.set(vcs_url)
                scm {
                    url.set(vcs_url)
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
                scm {
                    url.set(vcs_url)
                }
            }
        }
        afterEvaluate {
            gradlePlugin.plugins.forEach {
                val publicationName = "${it.name}PluginMarkerMaven"
                // we need to add this to meet Sonatype requirement
                named<MavenPublication>(publicationName) {
                    // An individual task for each plugin is created
                    // because in gradle 8 task cannot use output of another task
                    // without declaring a dependency on it.
                    // If we use the same task for multiple publications it will result in an error
                    // because Gradle will think the task uses the output from another task (for a different plugin).
                    // This happens because all files will have the same name
                    artifact(markerJarTask(it.name))
                    artifact(markerSourceJarTask(it.name)) {
                        classifier = "source"
                    }
                    artifact(markerJavadocJarTask(it.name)) {
                        classifier = "javadoc"
                    }
                }
            }
        }
    }
}

fun Project.markerJarTask(pluginName: String): TaskProvider<Jar> =
    tasks.register<Jar>("marker${pluginName.capitalizeAsciiOnly()}Jar") {
        archiveBaseName.set("marker-$pluginName")
        manifest.attributes(
            "Gradle-Plugin-Name" to pluginName,
        )
    }

fun Project.markerSourceJarTask(pluginName: String): TaskProvider<Jar> =
    tasks.register<Jar>("marker${pluginName.capitalizeAsciiOnly()}SourceJar") {
        archiveBaseName.set("marker-$pluginName-source")
        manifest.attributes(
            "Gradle-Plugin-Name" to pluginName,
        )
    }

fun Project.markerJavadocJarTask(pluginName: String): TaskProvider<Jar> =
    tasks.register<Jar>("marker${pluginName.capitalizeAsciiOnly()}JavadocJar") {
        archiveBaseName.set("marker-$pluginName-javadoc")
        manifest.attributes(
            "Gradle-Plugin-Name" to pluginName,
        )
    }

signing {
    val signingKey = findProperty("signingKey") as? String
    val signingPassword = findProperty("signingPassword") as? String
    useInMemoryPgpKeys(signingKey, signingPassword)
    publishing.publications.forEach(this@signing::sign)
}

tasks.withType<Sign> {
    onlyIf {
        project.hasProperty("signingKey") && project.hasProperty("signingPassword")
    }
}
