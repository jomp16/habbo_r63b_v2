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

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.zip.ZipFile

/**
 * Gera o `plugin.json` final do plugin: o descritor escrito à mão em `src/main/resources/plugin.json`
 * (nome, versão, autores) + `entryPoints`, a lista de subclasses concretas de `PluginListener`
 * encontradas no bytecode do plugin. O servidor carrega exatamente essas classes — sem scan de classpath.
 *
 * Superclasses fora do módulo (ex.: uma base no `:server`) são resolvidas pelo [compileClasspath].
 * Nenhum entry point encontrado falha o build: um JAR sem listener não faria nada.
 */
abstract class GeneratePluginDescriptorTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val descriptor: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classesDirs: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val compileClasspath: ConfigurableFileCollection

    @get:Input
    abstract val pluginName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        description = "Gera o plugin.json com os entryPoints (PluginListener) detectados no bytecode."
        group = "build"
    }

    @TaskAction
    fun generate() {
        val nodes = classesDirs.asFileTree.matching { include("**/*.class") }.files.sorted().map { file ->
            try {
                ClassNode().also { ClassReader(file.readBytes()).accept(it, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES) }
            } catch (e: Exception) {
                throw GradleException("Não foi possível ler a classe ${file.path} (ASM desatualizado para este JDK?)", e)
            }
        }
        val ownSuper = nodes.associate { it.name to it.superName }

        val zips = HashMap<File, ZipFile>()
        try {
            val classpathEntries = compileClasspath.files.filter { it.exists() }

            fun externalSuper(name: String): String? {
                val entry = "$name.class"
                for (root in classpathEntries) {
                    val bytes = if (root.isDirectory) {
                        File(root, entry).takeIf { it.isFile }?.readBytes()
                    } else {
                        val zip = zips.getOrPut(root) { ZipFile(root) }
                        zip.getEntry(entry)?.let { e -> zip.getInputStream(e).use { it.readBytes() } }
                    }
                    if (bytes != null) return ClassReader(bytes).superName
                }
                return null
            }

            fun superOf(name: String): String? = if (name in ownSuper) ownSuper[name] else externalSuper(name)

            fun isPluginListener(name: String): Boolean {
                var current = superOf(name)
                while (current != null) {
                    if (current == PLUGIN_LISTENER) return true
                    current = superOf(current)
                }
                return false
            }

            val entryPoints = nodes
                .filter { it.access and (Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE) == 0 }
                .filter { isPluginListener(it.name) }
                .map { it.name.replace('/', '.') }
                .sorted()

            if (entryPoints.isEmpty()) {
                throw GradleException("Plugin '${pluginName.get()}' não tem nenhuma subclasse concreta de PluginListener — nada a carregar")
            }

            @Suppress("UNCHECKED_CAST")
            val json = LinkedHashMap(JsonSlurper().parse(descriptor.get().asFile) as Map<String, Any?>)
            json["entryPoints"] = entryPoints

            val target = outputDir.get().asFile.resolve("plugin.json")
            target.parentFile.mkdirs()
            target.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(json)) + "\n")

            logger.lifecycle("🔌 ${pluginName.get()}: entryPoints = $entryPoints")
        } finally {
            zips.values.forEach { it.close() }
        }
    }

    private companion object {
        const val PLUGIN_LISTENER = "ovh/rwx/habbo/plugin/api/PluginListener"
    }
}
