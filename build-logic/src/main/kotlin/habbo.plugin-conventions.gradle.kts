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

// plugin.json final = descritor do src (nome/versão/autores) + entryPoints detectados no bytecode.
// O descritor-fonte fica em src/main/plugin/ (FORA de resources/) de propósito: o IntelliJ copia
// resources/ cru para out/production/resources e o rebel.xml põe essa pasta antes do JAR, então o
// JRebel serviria o plugin.json com ${...} e sem entryPoints no lugar do gerado.
val sourceDescriptor = layout.projectDirectory.file("src/main/plugin/plugin.json")

val generatePluginDescriptor = tasks.register<GeneratePluginDescriptorTask>("generatePluginDescriptor") {
    pluginName.set(pluginProjectName)
    descriptor.set(sourceDescriptor)
    classesDirs.from(tasks.named("compileKotlin"))
    compileClasspath.from(configurations.named("compileClasspath"))
    outputDir.set(layout.buildDirectory.dir("generated/plugin-descriptor"))
}

extensions.getByType<SourceSetContainer>().named("main") {
    resources.srcDir(generatePluginDescriptor.flatMap { it.outputDir })
}

tasks.named<ProcessResources>("processResources") {
    // expande ${project_name}/${version} do plugin.json gerado (e de outros recursos)
    eachFile(ExpandPluginResources(mapOf("project_name" to pluginProjectName, "version" to pluginVersion)))
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
