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
    alias(libs.plugins.buildconfig)
}

dependencies {
    testImplementation(libs.junit)
    implementation(libs.slf4j.api)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.coroutines)
    implementation(project(":pathfinding"))
    implementation(project(":camera"))
    api(project(":plugin_manager"))

    implementation(libs.netty.all)
    implementation(libs.netty.epoll)
    implementation(libs.kwery.core)
    implementation(libs.mariadb.driver)
    implementation(libs.hikari)
    implementation(libs.reflections)
    implementation(libs.commons.lang)
    implementation(libs.jasypt)
    implementation(libs.bouncycastle)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.yaml)
    implementation(libs.commons.codec)
    runtimeOnly(libs.nashorn)
}

buildConfig {
    packageName("ovh.rwx.habbo")

    val gitCommitFull = providers.provider {
        providers.exec {
            commandLine("git", "-C", rootProject.projectDir.absolutePath, "rev-parse", "HEAD")
        }.standardOutput.asText.get().trim()
    }
    val gitCommitShort = providers.provider {
        providers.exec {
            commandLine("git", "-C", rootProject.projectDir.absolutePath, "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
    }

    buildConfigField("NAME", "Habbo R63B v2")
    buildConfigField("VERSION", version.toString())
    buildConfigField("GIT_COMMIT_SHORT", gitCommitShort)
    buildConfigField("GIT_COMMIT_FULL", gitCommitFull)
    buildConfigField(
        "java.time.Instant",
        "BUILD_INSTANT",
        provider { "Instant.ofEpochMilli(${System.currentTimeMillis()}L)" })

    useKotlinOutput { internalVisibility = false }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Class-Path"] = configurations.runtimeClasspath.get().files.joinToString(" ") { it.name }
    }
}

val verifyPacketSignatures = tasks.register<VerifyPacketSignaturesTask>("verifyPacketSignatures") {
    classesDirs.from(tasks.named("compileKotlin"))
    failOnError.convention(!project.hasProperty("lenientPackets"))
    dependsOn("compileKotlin")
}

tasks.named("classes") {
    dependsOn(verifyPacketSignatures)
}
