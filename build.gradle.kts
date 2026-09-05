/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
 *
 * This file is part of habbo_r63b_v2.
 *
 * habbo_r63b_v2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b_v2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b_v2. If not, see <http://www.gnu.org/licenses/>.
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.dependency.updates)
}

// Shared build logic lives in build-logic; modules apply conventions explicitly.

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    filterConfigurations = Spec<Configuration> {
        val internalKotlinConfiguration = name in setOf(
            "kotlinCompilerClasspath",
            "kotlinBuildToolsApiClasspath",
            "kotlinAbiValidationCompatClasspath",
            "kotlinKlibCommonizerClasspath"
        )
        internalKotlinConfiguration.not() &&
                !(name.startsWith("kotlinCompilerPluginClasspath") && name != "kotlinCompilerPluginClasspath")
    }

    rejectVersionIf {
        val version = candidate.version.uppercase()
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.contains(it) }
        val stableNumericVersion = Regex("^[0-9]+(?:\\.[0-9]+)*(?:-R)?$").matches(version)
        !(stableKeyword || stableNumericVersion)
    }
}
