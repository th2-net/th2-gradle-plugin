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

import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.findByType
import org.gradle.plugins.signing.SigningPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class PublishTh2PluginTest {
    @Test
    fun `applies required plugins`() {
        val project =
            ProjectBuilder.builder()
                .build()

        project.pluginManager.apply("java")
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.exactpro.th2.gradle.publish")

        project.group = "com.example"
        project.version = "1.0.0"
        project.description = "test description"
        project.extensions.findByType<PublishTh2Extension>().apply {
            assertNotNull(this, "no publish th2 extension applied")
            sonatype {
                username.set("user")
                password.set("pwd")
            }
            signature {
                key.set("key")
                password.set("pwd")
            }
            pom {
                vcsUrl.set("https://test.com")
            }
        }

        // We need to think how to reorganize plugins to avoid calling evaluation in tests
        (project as DefaultProject).evaluate()

        project.run {
            assertAll(
                { assertNotNull(plugins.findPlugin(SigningPlugin::class.java), "no signing plugin configured") },
                {
                    val publications =
                        assertNotNull(extensions.findByType<PublishingExtension>(), "no publication extension configured")
                    val javaPublication: Publication =
                        assertNotNull(publications.publications.findByName("mavenJava"), "no publication configured")
                    assertIs<MavenPublication>(javaPublication, "not a maven publication")
                    val artifacts = javaPublication.artifacts
                    Assertions.assertEquals(3, artifacts.size) {
                        "unexpected number of artifacts: $artifacts"
                    }
                    assertAll(
                        { assertEquals("https://test.com", javaPublication.pom.url.get(), "unexpected url set") },
                        { assertEquals("test description", javaPublication.pom.description.get(), "unexpected description") },
                        { assertTrue(artifacts.any { it.classifier.isNullOrEmpty() }, "no main artifact") },
                        { assertTrue(artifacts.any { it.classifier == "sources" }, "no sources artifact") },
                        { assertTrue(artifacts.any { it.classifier == "javadoc" }, "no javadoc artifact") },
                    )
                },
                {
                    assertNotNull(
                        tasks.findByName("publishMavenJavaPublicationToSonatypeRepository"),
                        "not task to publish to sonatype",
                    )
                },
                {
                    assertNotNull(
                        tasks.findByName("closeAndReleaseSonatypeStagingRepository"),
                        "not task to close and release sonatype staging repository",
                    )
                },
            )
        }
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
            subProject.pluginManager.apply(PublishTh2Plugin::class.java)
        }
    }
}
