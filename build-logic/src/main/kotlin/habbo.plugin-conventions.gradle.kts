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

plugins {
    id("habbo.kotlin-conventions")
}

val mDestinationDirectory = rootProject.layout.projectDirectory.dir("plugins").asFile
val libDirName = "lib/${project.name}"
val pluginProjectName = project.name
val pluginVersion = project.version.toString()

val pluginLibs = configurations.named("runtimeClasspath").map { pluginRuntimeClasspath ->
    pluginRuntimeClasspath
        .minus(project(":server").configurations.named("runtimeClasspath").get())
        .filter { !it.name.startsWith("server-") }
}

tasks.named<ProcessResources>("processResources") {
    filesMatching(listOf("**/*.properties", "**/*.json", "**/*.yml", "**/*.yaml")) {
        expand(mapOf("project_name" to pluginProjectName, "version" to pluginVersion))
    }
}

dependencies {
    compileOnly(project(":server"))
}

tasks.named<Delete>("clean") {
    delete(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    delete(mDestinationDirectory.resolve(libDirName))
}

val copyLibs = tasks.register<Copy>("copyLibs") {
    from(pluginLibs)
    into(mDestinationDirectory.resolve(libDirName))
}

tasks.named<Jar>("jar") {
    destinationDirectory.set(mDestinationDirectory)
    dependsOn(copyLibs)
    manifest.attributes["Class-Path"] = project.providers.provider {
        pluginLibs.get().files.joinToString(" ") { "$libDirName/${it.name}" }
    }
}
