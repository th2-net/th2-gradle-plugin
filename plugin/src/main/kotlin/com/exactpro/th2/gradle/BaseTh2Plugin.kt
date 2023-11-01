package com.exactpro.th2.gradle

import com.github.jk1.license.LicenseReportExtension
import com.github.jk1.license.LicenseReportPlugin
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.JsonReportRenderer
import com.gorylenko.GitPropertiesPlugin
import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.owasp.dependencycheck.gradle.DependencyCheckPlugin
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import java.net.URI

private const val JAVA_VERSION_PROP = "java.version"

private const val JAVA_VENDOR_PROP = "java.vendor"

internal const val EXACTPRO_SYSTEMS_LLC = "Exactpro Systems LLC"

internal const val VENDOR_ID = "com.exactpro"

class BaseTh2Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        check(project == project.rootProject) {
            "th2 base plugin must be applied to the root project"
        }
        configureOwasp(project)
        configureLicenseReport(project)
        configureGitProperties(project)
        lockDependencies(project)
        if (project.subprojects.isEmpty()) {
            project.afterEvaluate {
                configureManifestInfo()
            }
        } else {
            project.subprojects {
                afterEvaluate {
                    configureManifestInfo()
                }
            }
        }
    }

    private fun lockDependencies(project: Project) {
        project.dependencyLocking {
            lockAllConfigurations()
        }
    }

    private fun configureOwasp(project: Project) {
        project.pluginManager.apply(DependencyCheckPlugin::class.java)
        project.extensions.getByType<DependencyCheckExtension>().apply {
            formats = listOf("SARIF", "JSON", "HTML")
            failBuildOnCVSS = 5.0f

            analyzers.apply {
                assemblyEnabled = false
                nugetconfEnabled = false
                nodeEnabled = false
            }
        }
    }

    private fun configureLicenseReport(project: Project) {
        project.pluginManager.apply(LicenseReportPlugin::class.java)
        project.extensions.getByType<LicenseReportExtension>().apply {
            val licenseNormalizerBundlePath = project.buildDir.resolve("license-normalizer-bundle.json")

            if (!licenseNormalizerBundlePath.exists()) {
                DownloadAction(project).apply {
                    src("https://raw.githubusercontent.com/th2-net/.github/main/license-compliance/gradle-license-report/license-normalizer-bundle.json")
                    dest(licenseNormalizerBundlePath)
                    overwrite(false)
                }.execute().get()
            }

            filters = arrayOf(LicenseBundleNormalizer(licenseNormalizerBundlePath.path, false))
            renderers = arrayOf(JsonReportRenderer("licenses.json", false))
            excludeOwnGroup = true
            allowedLicensesFile = URI("https://raw.githubusercontent.com/th2-net/.github/main/license-compliance/gradle-license-report/allowed-licenses.json")
        }
    }

    private fun configureGitProperties(project: Project) {
        project.pluginManager.apply(GitPropertiesPlugin::class.java)
    }

    private fun Project.configureManifestInfo() {
        plugins.withType<JavaPlugin> {
            tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME) {
                manifest {
                    val javaInfo = "${System.getProperty(JAVA_VERSION_PROP)} (${System.getProperty(JAVA_VENDOR_PROP)})"
                    attributes(
                        mapOf(
                            "Created-By" to javaInfo,
                            "Specification-Title" to "",
                            "Specification-Vendor" to EXACTPRO_SYSTEMS_LLC,
                            "Implementation-Title" to this@configureManifestInfo.name,
                            "Implementation-Vendor" to EXACTPRO_SYSTEMS_LLC,
                            "Implementation-Vendor-Id" to VENDOR_ID,
                            "Implementation-Version" to this@configureManifestInfo.version
                        )
                    )
                }
            }
        }
    }
}