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
    id("java-library-distribution")
}

group = "ovh.rwx"

dependencies {
    testImplementation(libs.junit)
    implementation(libs.slf4j.api)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.coroutines)
    implementation(project(":server"))

    //noinspection GrUnresolvedAccess
    implementation(libs.log4j.core)

    //noinspection GrUnresolvedAccess
    implementation(libs.log4j.slf4j)
    implementation(libs.log4j.slf4j2)


    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.yaml)
}

tasks.named<Jar>("jar") {
    //noinspection GrUnresolvedAccess, GroovyAssignabilityCheck
    dependsOn(
        project(":plugin_impl1").tasks.named("jar"),
        project(":plugin_webapp").tasks.named("jar")
    )

    manifest {
        //noinspection GrUnresolvedAccess
        attributes["Main-Class"] = "ovh.rwx.MainKt"
        attributes["Class-Path"] =
            (configurations.runtimeClasspath.get().files - project(":server").configurations.runtimeClasspath.get().files)
                .joinToString(" ") { "lib/${it.name}" }
    }
}

distributions {
    main {
        contents {
            into("/") {
                from(rootProject.file("config.example.yaml"))
                from(rootProject.file("fastfood_localization.json"))

                into("/plugins") {
                    from(rootProject.file("plugins"))
                }
            }
        }
    }
}

//noinspection GrUnresolvedAccess
tasks.named<Tar>("distTar") {
    compression = Compression.BZIP2
}
