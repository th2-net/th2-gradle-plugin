/*
 * Copyright 2024 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.gradle

import com.exactpro.th2.gradle.config.Libraries
import com.github.jk1.license.LicenseReportExtension
import com.github.jk1.license.LicenseReportPlugin
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.JsonReportRenderer
import com.gorylenko.GitPropertiesPlugin
import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaTestFixturesPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.owasp.dependencycheck.gradle.DependencyCheckPlugin
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import java.net.URL

private const val JAVA_VERSION_PROP = "java.version"

private const val JAVA_VENDOR_PROP = "java.vendor"

private const val BASE_EXTERNAL_CONFIGURATION_URL = "https://raw.githubusercontent.com/th2-net/.github/main"

internal const val EXACTPRO_SYSTEMS_LLC = "Exactpro Systems LLC"

internal const val VENDOR_ID = "com.exactpro"

internal const val TEST_FIXTURES_IMPLEMENTATION = "testFixturesImplementation"

class BaseTh2Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        check(project === project.rootProject) {
            "th2 base plugin must be applied to the root project"
        }
        configureOwasp(project)
        configureLicenseReport(project)
        configureGitProperties(project)
        lockDependencies(project)
        project.allprojects {
            configureManifestInfo()
            configureBomDependency()
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

            nvd.apply {
                apiKey = project.findProperty("nvdApiKey") as? String
                delay = (project.findProperty("nvdDelay") as? String)?.toInt() ?: 10_000
            }

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
                    src("$BASE_EXTERNAL_CONFIGURATION_URL/license-compliance/gradle-license-report/license-normalizer-bundle.json")
                    dest(licenseNormalizerBundlePath)
                    overwrite(false)
                }.execute().get()
            }

            filters =
                arrayOf(
                    LicenseBundleNormalizer(licenseNormalizerBundlePath.path, false),
                )
            renderers = arrayOf(JsonReportRenderer("licenses.json", false))
            excludeOwnGroup = true
            allowedLicensesFile = URL("$BASE_EXTERNAL_CONFIGURATION_URL/license-compliance/gradle-license-report/allowed-licenses.json")
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
                            "Implementation-Version" to this@configureManifestInfo.version,
                        ),
                    )
                }
            }
        }
    }

    private fun Project.configureBomDependency() {
        // only if we have Java plugin applied
        plugins.withType<JavaPlugin> {
            val project = this@configureBomDependency
            project.dependencies
                .add(
                    JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
                    project.dependencies.platform(Libraries.TH2_BOM),
                )

            // only if we have JavaTestFixtures plugin applied
            plugins.withType<JavaTestFixturesPlugin> {
                project.dependencies
                    .add(
                        TEST_FIXTURES_IMPLEMENTATION,
                        project.dependencies.platform(Libraries.TH2_BOM),
                    )
            }
        }
    }
}
