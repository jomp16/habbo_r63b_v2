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
 * Write-behind: `db {}`/`dbTransaction {}` são JDBC SÍNCRONOS (jdbi.withHandle na thread
 * do caller). Writes chamados de handlers/threads Netty devem passar pelo
 * `WriteBehindManager.queue` (o método de escrita do DAO se auto-enfileira) — JDBC bloqueante
 * na event loop gera jitter para todos os jogadores. Reads podem ser síncronos, e os
 * "guaranteed immediate flushes" (logout/unload/shutdown, dentro do WriteBehindManager) rodam
 * em threads que podem bloquear.
 *
 * Como `db {}` é uma inline function, a referência a `Jdbi.withHandle`/`inTransaction`
 * aparece INLINED no bytecode do caller. Este scan proíbe essas referências fora do pacote
 * `ovh.rwx.habbo.database` (+ whitelist de boot), tornando impossível escrever `db {}`
 * ad-hoc em handler, manager ou jogo.
 */
abstract class VerifySyncJdbiAccessTask : DefaultTask() {

    @get:InputFiles
    abstract val classesDirs: ConfigurableFileCollection

    @get:Input
    abstract val allowedPackages: SetProperty<String>

    @get:Input
    abstract val allowedCallers: SetProperty<String>

    init {
        description = "Proíbe JDBC síncrono (db{}/dbTransaction{} inline em Jdbi.withHandle) fora do pacote database — writes vão pelo WriteBehindManager.queue."
        group = "verification"
    }

    private val jdbiInternal = "org/jdbi/v3/core/Jdbi"
    private val syncMethods = setOf("withHandle", "inTransaction")

    data class Violation(
        val callerClass: String,
        val accessor: String,
        val sourceFile: String?,
        val lineNumber: Int,
    )

    @TaskAction
    fun verify() {
        val classFiles = classesDirs.asFileTree.filter { it.extension == "class" }.files
        val packages = allowedPackages.get()
        val whitelist = allowedCallers.get()
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
            if (packages.any { callerInternal.startsWith(it) }) continue

            for (method in node.methods.orEmpty()) {
                var currentLine = -1
                for (insn in method.instructions ?: continue) {
                    when (insn) {
                        is LineNumberNode -> currentLine = insn.line
                        is MethodInsnNode -> {
                            if (insn.owner == jdbiInternal && insn.name in syncMethods) {
                                violations += Violation(callerInternal, "Jdbi.${insn.name}()", node.sourceFile, currentLine)
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            val report = violations.joinToString("\n") {
                "  - ${it.callerClass} (${it.sourceFile}:${it.lineNumber}) usou ${it.accessor}"
            }
            throw GradleException(
                "❌ JDBC síncrono detectado fora do pacote database (${violations.size}):\n$report\n\n" +
                        "Writes chamados de thread Netty/jogo devem se auto-enfileirar no DAO via WriteBehindManager.queue.\n" +
                        "Leia síncrono é permitido; flush garantido (logout/unload/shutdown) roda no WriteBehindManager.\n" +
                        "Se este acesso é legítimo (boot), adicione a classe à whitelist em server/build.gradle.kts."
            )
        }

        logger.lifecycle("✅ JDBC síncrono restrito ao pacote database + whitelist (write-behind preservado).")
    }
}
