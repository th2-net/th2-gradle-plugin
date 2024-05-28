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

import com.github.jk1.license.LicenseReportExtension
import com.github.jk1.license.LicenseReportPlugin
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.gorylenko.GitPropertiesPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.owasp.dependencycheck.gradle.DependencyCheckPlugin
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(PER_CLASS)
internal class BaseTh2PluginTest {
    @ParameterizedTest(name = "when {0} plugin applied")
    @MethodSource("requiredPlugins")
    fun `applies required plugins`(javaPlugin: String) {
        val project =
            ProjectBuilder.builder()
                .build()

        project.pluginManager.apply(javaPlugin)
        project.pluginManager.apply("com.exactpro.th2.gradle.base")
        assertAll(
            { assertHasPlugin(project, DependencyCheckPlugin::class.java) },
            { assertHasPlugin(project, LicenseReportPlugin::class.java) },
            { assertHasPlugin(project, GitPropertiesPlugin::class.java) },
            { assertHasBomDependency(project, "implementation") },
        )
    }

    @ParameterizedTest(name = "when {0} required plugin applied")
    @MethodSource("requiredPlugins")
    fun `applies test fixtures plugin`(javaPlugin: String) {
        val project =
            ProjectBuilder.builder()
                .build()

        project.pluginManager.apply(javaPlugin)
        project.pluginManager.apply("java-test-fixtures")
        project.pluginManager.apply("com.exactpro.th2.gradle.base")
        assertAll(
            { assertHasPlugin(project, DependencyCheckPlugin::class.java) },
            { assertHasPlugin(project, LicenseReportPlugin::class.java) },
            { assertHasPlugin(project, GitPropertiesPlugin::class.java) },
            { assertHasBomDependency(project, "implementation") },
            { assertHasBomDependency(project, "testFixturesImplementation") },
        )
    }

    @Test
    fun `reports error if applied not to the root project`() {
        val root =
            ProjectBuilder.builder()
                .build()
        val subProject =
            ProjectBuilder.builder()
                .withParent(root)
                .build()

        assertThrows<Exception> {
            subProject.pluginManager.apply(BaseTh2Plugin::class.java)
        }
    }

    @Test
    fun `applies license configuration from build script`() {
        val project =
            ProjectBuilder.builder().build()

        val customAllowedLicense = "file:test-allowed-licenses.json"
        val customLicenseNormalizer = Path("src", "test", "resources", "test-license-normalizer-bundle.json").absolutePathString()
        project.extensions.extraProperties.set(TH2_LICENCE_ALLOW_LICENCE_URL_PROP, customAllowedLicense)
        project.extensions.extraProperties.set(TH2_LICENCE_LICENSE_NORMALIZER_BUNDLE_PATH_PROP, customLicenseNormalizer)
        project.pluginManager.apply("com.exactpro.th2.gradle.base")

        project.run {
            assertAll(
                {
                    val licenseReport =
                        assertNotNull(
                            extensions.findByType<LicenseReportExtension>(),
                            "no license report extension configured",
                        )
                    val licenseBundleNormalizer =
                        assertNotNull(
                            licenseReport.filters.single { it is LicenseBundleNormalizer },
                            "no LicenseBundleNormalizer filter",
                        ) as LicenseBundleNormalizer
                    assertAll(
                        { assertEquals(URL(customAllowedLicense), licenseReport.allowedLicensesFile, "allowed license URL isn't applied") },
                        {
                            assertContains(
                                charSequence = licenseBundleNormalizer.getFilterConfigForCache(),
                                other = customLicenseNormalizer,
                                message = "LicenseBundleNormalizer filter isn't default",
                            )
                        },
                    )
                },
            )
        }
    }

    private fun <T : Plugin<*>> assertHasPlugin(
        project: Project,
        pluginClass: Class<T>,
    ) {
        assertTrue(
            project.plugins.hasPlugin(pluginClass),
            "no $pluginClass applied",
        )
    }

    private fun assertHasBomDependency(
        project: Project,
        configuration: String,
    ) {
        val bom =
            assertNotNull(
                project.configurations.findByName(configuration),
                "no $configuration configuration found",
            ).allDependencies
                .find {
                    it.group == "com.exactpro.th2" &&
                        it.name == "bom"
                }
        assertNotNull(bom, "bom not found in the $configuration configuration")
    }

    private fun requiredPlugins() = listOf("java", "java-library", "org.jetbrains.kotlin.jvm")
}
