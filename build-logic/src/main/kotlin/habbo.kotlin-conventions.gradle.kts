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

import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("idea")
}

group = "ovh.rwx"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

extensions.configure<JavaPluginExtension> {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

extensions.configure<IdeaModel> {
    module {
        sourceDirs.add(layout.buildDirectory.dir("gen/buildconfig/src/main").get().asFile)
        generatedSourceDirs.add(layout.buildDirectory.dir("gen/buildconfig/src/main").get().asFile)
    }
}

tasks.withType<Jar>().configureEach {
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.MF", "rebel.xml")
}
