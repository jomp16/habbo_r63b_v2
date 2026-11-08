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
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.mariadb)
    implementation(libs.slf4j.api)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.coroutines)
    implementation(project(":pathfinding"))
    implementation(project(":camera"))
    api(project(":plugin_manager"))

    implementation(libs.netty.all)
    implementation(libs.netty.epoll)
    implementation(libs.jdbi.core)
    implementation(libs.jdbi.kotlin)
    implementation(libs.jdbi.sqlobject)
    implementation(libs.jdbi.kotlin.sqlobject)
    implementation(libs.flyway.core)
    implementation(libs.flyway.mysql)
    implementation(libs.mariadb.driver)
    implementation(libs.hikari)
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

val verifySyncJdbiAccess = tasks.register<VerifySyncJdbiAccessTask>("verifySyncJdbiAccess") {
    classesDirs.from(tasks.named("compileKotlin"))
    // Write-behind: JDBC síncrono (db{}) só dentro do pacote database — writes de handler
    // Netty se auto-enfileiram no DAO via WriteBehindManager.queue
    allowedPackages.convention(setOf("ovh/rwx/habbo/database"))
    allowedCallers.convention(
        setOf(
            // Boot: loadPermissions() roda no start, antes de aceitar conexões
            "ovh/rwx/habbo/HabboServer",
            "ovh/rwx/habbo/game/permission/PermissionManager",
        )
    )
    dependsOn("compileKotlin")
}

val verifySessionReleaseAccess = tasks.register<VerifySessionReleaseAccessTask>("verifySessionReleaseAccess") {
    classesDirs.from(tasks.named("compileKotlin"))
    // Seam de wire + handshake + diagnóstico: os únicos que podem tocar HabboSession.release
    allowedCallers.convention(
        setOf(
            "ovh/rwx/habbo/communication/HabboHandler",
            "ovh/rwx/habbo/netty/HabboNettyDecoder",
            "ovh/rwx/habbo/netty/HabboNettyEncoder",
            // Seam de wire: agrupa sessions por header-ID p/ clone de protótipo no broadcast de quarto
            "ovh/rwx/habbo/game/room/managers/RoomNetworkDispatcher",
            "ovh/rwx/habbo/communication/incoming/handshake/HandshakeReleaseCheckHandler",
            // Diagnóstico: comando about exibe a release ao admin
            "ovh/rwx/habbo/plugin/listeners/room/about/RoomAboutCommandListener",
        )
    )
    dependsOn("compileKotlin")
}

// Índice estático de handlers/interactors (substitui o scan de classpath no boot e no reload_handlers).
// Vira resource do módulo: vai no jar e, em dev, é relido do disco a cada reload.
val generateClassIndex = tasks.register<GenerateClassIndexTask>("generateClassIndex") {
    classesDirs.from(tasks.named("compileKotlin"))
    outputDir.set(layout.buildDirectory.dir("generated/class-index"))
    methodAnnotations.putAll(
        mapOf(
            "HANDLER" to "Lovh/rwx/habbo/communication/Handler;",
            "HANDLER_R63A" to "Lovh/rwx/habbo/communication/HandlerR63A;",
            "RESPONSE" to "Lovh/rwx/habbo/communication/Response;",
            "RESPONSE_R63A" to "Lovh/rwx/habbo/communication/ResponseR63A;",
            "FF_HANDLER" to "Lovh/rwx/fastfood/communication/FFHandler;",
            "FF_RESPONSE" to "Lovh/rwx/fastfood/communication/FFResponse;",
        )
    )
    classAnnotations.put("WIRED_ITEM", "Lovh/rwx/habbo/game/item/wired/WiredItemInteractor;")
    subclassesOf.putAll(
        mapOf(
            "ITEM_INTERACTOR" to "ovh/rwx/habbo/game/item/ItemInteractor",
            "FURNITURE_LOGIC" to "ovh/rwx/habbo/game/item/logic/FurnitureLogic",
            "PLUGIN_LISTENER" to "ovh/rwx/habbo/plugin/api/PluginListener",
        )
    )
    requiredKinds.addAll(
        "HANDLER", "HANDLER_R63A", "RESPONSE", "RESPONSE_R63A", "FF_HANDLER", "FF_RESPONSE",
        "WIRED_ITEM", "ITEM_INTERACTOR", "FURNITURE_LOGIC", "PLUGIN_LISTENER",
    )
}

sourceSets.named("main") {
    resources.srcDir(generateClassIndex.flatMap { it.outputDir })
}

tasks.named("classes") {
    dependsOn(verifyPacketSignatures)
    dependsOn(verifySessionReleaseAccess)
    dependsOn(verifySyncJdbiAccess)
}
