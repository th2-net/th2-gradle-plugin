/*
 * Copyright 2024-2025 Exactpro (Exactpro Systems Limited)
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

import com.github.jk1.license.LicenseReportPlugin
import com.gorylenko.GitPropertiesPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.owasp.dependencycheck.gradle.DependencyCheckPlugin
import org.owasp.dependencycheck.gradle.extension.AnalyzerExtension
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import org.owasp.dependencycheck.gradle.extension.NvdExtension
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(PER_CLASS)
internal class BaseTh2PluginTest {
    @ParameterizedTest(name = "when {0} plugin applied")
    @MethodSource("requiredPlugins")
    fun `applies required plugins`(javaPlugin: String) {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply(javaPlugin)
        project.pluginManager.apply("com.exactpro.th2.gradle.base")
        assertAll(
            { assertHasPlugin(project, DependencyCheckPlugin::class.java) },
            { assertHasPlugin(project, LicenseReportPlugin::class.java) },
            { assertHasPlugin(project, GitPropertiesPlugin::class.java) },
            { assertHasBomDependency(project, "implementation") },
            { assertDependencyCheck(project.the<DependencyCheckExtension>()) },
        )
    }

    @ParameterizedTest(name = "when {0} required plugin applied")
    @MethodSource("requiredPlugins")
    fun `applies test fixtures plugin`(javaPlugin: String) {
        val project = ProjectBuilder.builder().build()

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
            ProjectBuilder.builder().build()
        val subProject = ProjectBuilder.builder().withParent(root).build()

        assertThrows<Exception> {
            subProject.pluginManager.apply(BaseTh2Plugin::class.java)
        }
    }

    @Test
    fun `apply properties for dependency check extension`() {
        val nvdApiKey = "test-nvdApiKey"
        val nvdDelay = 9_999
        val nvdDatafeedUrl = "https://nvdDatafeedUrl.test"
        val nvdDatafeedUser = "test-nvdDatafeedUser"
        val nvdDatafeedPassword = "test-nvdDatafeedPassword"
        val analyzersOssIndexUser = "test-analyzersOssIndexUser"
        val analyzersOssIndexToken = "test-analyzersOssIndexToken"
        val analyzersKnownExploitedURL = "https://knownExploitedURL.test"
        val analyzersKnownExploitedUser = "test-analyzersKnownExploitedUser"
        val analyzersKnownExploitedPassword = "test-analyzersKnownExploitedPassword"
        val project = ProjectBuilder.builder().build()

        project.extensions.extraProperties["nvdApiKey"] = nvdApiKey
        project.extensions.extraProperties["nvdDelay"] = nvdDelay.toString()
        project.extensions.extraProperties["nvdDatafeedUrl"] = nvdDatafeedUrl
        project.extensions.extraProperties["nvdDatafeedUser"] = nvdDatafeedUser
        project.extensions.extraProperties["nvdDatafeedPassword"] = nvdDatafeedPassword
        project.extensions.extraProperties["analyzersOssIndexUser"] = analyzersOssIndexUser
        project.extensions.extraProperties["analyzersOssIndexToken"] = analyzersOssIndexToken
        project.extensions.extraProperties["analyzersKnownExploitedURL"] = analyzersKnownExploitedURL
        project.extensions.extraProperties["analyzersKnownExploitedUser"] = analyzersKnownExploitedUser
        project.extensions.extraProperties["analyzersKnownExploitedPassword"] = analyzersKnownExploitedPassword
        project.pluginManager.apply("com.exactpro.th2.gradle.base")

        val dependencyCheckExtension = project.the<DependencyCheckExtension>()
        val nvd = dependencyCheckExtension.nvd
        val analyzers = dependencyCheckExtension.analyzers
        assertAll(
            { assertEquals(nvdApiKey, nvd.apiKey.get(), "unexpected dependencyCheck.nvd.apiKey") },
            { assertEquals(nvdDelay, nvd.delay.get(), "unexpected dependencyCheck.nvd.nvdDelay") },
            { assertEquals(nvdDatafeedUrl, nvd.datafeedUrl.get(), "unexpected dependencyCheck.nvd.datafeedUrl") },
            { assertEquals(nvdDatafeedUser, nvd.datafeedUser.get(), "unexpected dependencyCheck.nvd.datafeedUser") },
            { assertEquals(nvdDatafeedPassword, nvd.datafeedPassword.get(), "unexpected dependencyCheck.nvd.datafeedPassword") },
            {
                assertEquals(
                    analyzersOssIndexUser,
                    analyzers.ossIndex.username.get(),
                    "unexpected dependencyCheck.analyzers.ossIndex.username",
                )
            },
            {
                assertEquals(
                    analyzersOssIndexToken,
                    analyzers.ossIndex.password.get(),
                    "unexpected dependencyCheck.analyzers.ossIndex.password",
                )
            },
            {
                assertEquals(
                    analyzersKnownExploitedURL,
                    analyzers.kev.url.get(),
                    "unexpected dependencyCheck.analyzers.kev.url",
                )
            },
            {
                assertEquals(
                    analyzersKnownExploitedUser,
                    analyzers.kev.user.get(),
                    "unexpected dependencyCheck.analyzers.kev.user",
                )
            },
            {
                assertEquals(
                    analyzersKnownExploitedPassword,
                    analyzers.kev.password.get(),
                    "unexpected dependencyCheck.analyzers.kev.password",
                )
            },
        )
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

    @Suppress("unused")
    private fun requiredPlugins() = listOf("java", "java-library", "org.jetbrains.kotlin.jvm")

    private fun assertDependencyCheck(extension: DependencyCheckExtension) =
        assertAll(
            {
                assertEquals(
                    listOf("SARIF", "JSON", "HTML"),
                    extension.formats.get(),
                    "unexpected dependency check formats",
                )
            },
            {
                assertEquals(
                    5.0f,
                    extension.failBuildOnCVSS.get(),
                    "unexpected dependency check failBuildOnCVSS",
                )
            },
            { assertNvd(extension.nvd) },
            { assertAnalyzers(extension.analyzers) },
        )

    private fun assertAnalyzers(extension: AnalyzerExtension) =
        assertAll(
            { assertFalse(extension.assemblyEnabled.get(), "unexpected analyzers.assemblyEnabled") },
            { assertFalse(extension.nugetconfEnabled.get(), "unexpected analyzers.nugetconfEnabled") },
            { assertFalse(extension.nodePackage.enabled.get(), "unexpected analyzers.nodeEnabled") },
            { assertNull(extension.kev.url.get(), "unexpected analyzers.knownExploitedURL") },
        )

    private fun assertNvd(extension: NvdExtension) =
        assertAll(
            { assertNull(extension.apiKey, "unexpected dependency check nvd.apiKey") },
            { assertEquals(10_000, extension.delay.get(), "unexpected dependency check nvd.delay") },
            { assertNull(extension.datafeedUrl, "unexpected dependency check nvd.datafeedUrl") },
        )
}
