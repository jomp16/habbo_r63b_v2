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
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.*

/**
 * HabboSession.release é a chave de transporte das tabelas de header (lookup no banco),
 * NÃO um conceito de versão. Lógica de era/feature deve usar session.protocolEra,
 * isPreShuffle ou predicados temporais (isVersionAtLeast/Before) — nunca a release string.
 *
 * Kotlin não tem package-private (internal é escopo de módulo, e handlers estão no mesmo
 * módulo), então o enforcement é este scan de bytecode: só as classes do seam de wire
 * (netty, HabboHandler, handshake) e o comando de diagnóstico podem acessar release.
 */
abstract class VerifySessionReleaseAccessTask : DefaultTask() {

    @get:InputFiles
    abstract val classesDirs: ConfigurableFileCollection

    @get:Input
    abstract val allowedCallers: SetProperty<String>

    init {
        description = "Proíbe acesso a HabboSession.release fora do seam de wire — use protocolEra/isVersion* ou HabboHandler.canSend."
        group = "verification"
    }

    private val habboSessionInternal = "ovh/rwx/habbo/game/user/HabboSession"

    data class Violation(
        val callerClass: String,
        val accessor: String,
        val sourceFile: String?,
        val lineNumber: Int,
    )

    @TaskAction
    fun verify() {
        val classFiles = classesDirs.asFileTree.filter { it.extension == "class" }.files
        val whitelist = allowedCallers.get() + habboSessionInternal
        val violations = mutableListOf<Violation>()

        for (classFile in classFiles) {
            val node = try {
                ClassReader(classFile.inputStream().readBytes()).let { reader ->
                    ClassNode().also { reader.accept(it, 0) }
                }
            } catch (e: Exception) {
                continue
            }

            val callerInternal = node.name
            if (callerInternal in whitelist) continue

            for (method in node.methods.orEmpty()) {
                var currentLine = -1
                for (insn in method.instructions ?: continue) {
                    when (insn) {
                        is LineNumberNode -> currentLine = insn.line
                        is MethodInsnNode -> {
                            if (insn.owner == habboSessionInternal && insn.name in ACCESSORS) {
                                violations += Violation(callerInternal, "${insn.name}()", node.sourceFile, currentLine)
                            }
                        }
                        is FieldInsnNode -> {
                            if (insn.owner == habboSessionInternal && insn.name == "release") {
                                violations += Violation(callerInternal, "${insn.opcodeName} release", node.sourceFile, currentLine)
                            }
                        }
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            val report = violations.joinToString("\n") {
                "  ${it.callerClass} → HabboSession.${it.accessor}  (${it.sourceFile}:${it.lineNumber})"
            }
            logger.error(
                "HabboSession.release acessado fora do seam de wire (chave de transporte das tabelas de header).\n" +
                        "Use session.protocolEra / isPreShuffle / isVersionAtLeast(Before) ou HabboHandler.canSend:\n" + report
            )
            throw GradleException("${violations.size} acesso(s) indevido(s) a HabboSession.release")
        }

        logger.lifecycle("✅ Acesso a HabboSession.release restrito ao seam de wire ({} classes na whitelist).", whitelist.size)
    }

    private val FieldInsnNode.opcodeName: String
        get() = when (opcode) {
            org.objectweb.asm.Opcodes.GETFIELD -> "leitura de"
            org.objectweb.asm.Opcodes.PUTFIELD -> "escrita de"
            else -> "acesso a"
        }

    private companion object {
        val ACCESSORS = setOf("getRelease", "setRelease")
    }
}
