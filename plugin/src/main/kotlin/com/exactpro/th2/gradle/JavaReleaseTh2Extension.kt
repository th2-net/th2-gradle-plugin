/*
 * Copyright 2025 Exactpro (Exactpro Systems Limited)
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

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class JavaReleaseTh2Extension
    @Inject
    constructor(
        project: Project,
    ) {
        /**
         * Defines the target JVM version to compile sources for.
         * Uses, [JavaVersion.VERSION_11] by default
         */
        val targetJavaVersion: Property<JavaVersion> =
            project.objects
                .property<JavaVersion>()
                .convention(JavaVersion.VERSION_11)
    }
