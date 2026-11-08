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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * Gera em build-time o índice estático de classes que antes era descoberto por ClassGraph no boot.
 *
 * Formato (`META-INF/habbo/class-index.txt`): uma linha `KIND<TAB>nome.binario.da.Classe` por entrada,
 * ordenadas. Dois tipos de regra:
 *  - [methodAnnotations]: classes com ao menos um MÉTODO anotado (descritor JVM `Lpkg/Anno;`)
 *  - [classAnnotations]: classes anotadas
 *  - [subclassesOf]: subclasses concretas (transitivas, sem abstract/interface) de um tipo base
 *
 * Um KIND declarado em [requiredKinds] que fique vazio falha o build: é o sinal de que a análise
 * quebrou (ex.: ASM sem suporte ao class file do JDK) e o servidor subiria sem handlers.
 */
abstract class GenerateClassIndexTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classesDirs: ConfigurableFileCollection

    /** KIND -> descritores JVM de anotações de método (`Lovh/rwx/Foo;`). */
    @get:Input
    abstract val methodAnnotations: MapProperty<String, String>

    /** KIND -> descritor JVM da anotação de classe. */
    @get:Input
    abstract val classAnnotations: MapProperty<String, String>

    /** KIND -> nome interno do tipo base (`ovh/rwx/Foo`). */
    @get:Input
    abstract val subclassesOf: MapProperty<String, String>

    @get:Input
    abstract val requiredKinds: org.gradle.api.provider.SetProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        description = "Gera o índice estático de handlers/interactors (substitui o scan de classpath em runtime)."
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

        val superOf = nodes.associate { it.name to it.superName }
        val entries = sortedSetOf<Pair<String, String>>(compareBy({ it.first }, { it.second }))

        fun isConcrete(node: ClassNode) = node.access and (Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE) == 0

        fun inherits(name: String, base: String): Boolean {
            var current = superOf[name]
            while (current != null) {
                if (current == base) return true
                current = superOf[current]
            }
            return false
        }

        for (node in nodes) {
            val binaryName = node.name.replace('/', '.')

            for ((kind, desc) in methodAnnotations.get()) {
                val annotated = node.methods.any { m ->
                    (m.visibleAnnotations.orEmpty() + m.invisibleAnnotations.orEmpty()).any { it.desc == desc }
                }
                if (annotated) entries += kind to binaryName
            }

            for ((kind, desc) in classAnnotations.get()) {
                if ((node.visibleAnnotations.orEmpty() + node.invisibleAnnotations.orEmpty()).any { it.desc == desc }) {
                    entries += kind to binaryName
                }
            }

            if (isConcrete(node)) {
                for ((kind, base) in subclassesOf.get()) {
                    if (inherits(node.name, base)) entries += kind to binaryName
                }
            }
        }

        val present = entries.map { it.first }.toSet()
        val missing = requiredKinds.get() - present
        if (missing.isNotEmpty()) {
            throw GradleException("Índice de classes sem nenhuma entrada para: ${missing.sorted().joinToString()} — análise de bytecode quebrada?")
        }

        val target = outputDir.get().asFile.resolve("META-INF/habbo/class-index.txt")
        target.parentFile.mkdirs()
        target.writeText(entries.joinToString("\n", postfix = "\n") { "${it.first}\t${it.second}" })

        logger.lifecycle("📇 Índice de classes: ${entries.size} entradas (${entries.groupingBy { it.first }.eachCount().toSortedMap()})")
    }
}
