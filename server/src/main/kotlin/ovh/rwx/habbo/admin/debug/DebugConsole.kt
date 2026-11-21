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

package ovh.rwx.habbo.admin.debug

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.user.HabboSession

/**
 * `debug` em texto (Server Console do messenger e stdin): `debug` lista, `debug <ferramenta> param=valor ...`.
 * Um valor vai até o próximo `param=` da própria ferramenta, então `sql=SELECT * FROM rooms WHERE id=5` funciona.
 */
object DebugConsole {
    /** Tamanho de cada mensagem do messenger (o cliente corta mensagens longas). */
    private const val CHUNK = 900
    private const val MAX_CHUNKS = 20

    /** Parse de `param=valor ...` usando só os nomes de parâmetro de [tool]. */
    fun parseArgs(tool: DebugTool, text: String): Map<String, String> {
        val names = tool.params.map { it.name }.toSet()
        val out = LinkedHashMap<String, String>()
        var key: String? = null
        val value = StringBuilder()

        for (token in text.split(' ').filter { it.isNotEmpty() }) {
            val eq = token.indexOf('=')
            val candidate = if (eq > 0) token.substring(0, eq) else null
            if (candidate != null && candidate in names) {
                key?.let { out[it] = value.toString() }
                key = candidate
                value.setLength(0)
                value.append(token.substring(eq + 1))
            } else {
                if (key == null) {
                    // sem `param=`: vale para o único parâmetro obrigatório (ex.: debug room_inspect 123)
                    key = tool.params.singleOrNull { it.required }?.name
                        ?: throw DebugToolException("use param=valor (${tool.params.joinToString { it.name }})")
                } else value.append(' ')
                value.append(token)
            }
        }
        key?.let { out[it] = value.toString() }

        return out
    }

    fun help(rank: Int): String {
        val visible = DebugTools.visibleTo(rank, HabboServer.habboConfig.debugToolsConfig)
        if (visible.isEmpty()) return "Nenhuma ferramenta de debug para o seu rank."
        return "debug <ferramenta> param=valor ...\n" + visible.joinToString("\n") { t ->
            "${t.name}" + t.params.joinToString("") { p -> " ${if (p.required) "" else "["}${p.name}${if (p.required) "" else "]"}" }
        }
    }

    fun execute(session: HabboSession, input: String): List<String> {
        val user = session.userInformation
        return execute(DebugCaller(user.id, user.username, user.rank, "console"), input)
    }

    /** Executa e devolve a resposta em pedaços prontos para o messenger. */
    fun execute(caller: DebugCaller, input: String): List<String> {
        val name = input.substringBefore(' ').trim()
        if (name.isEmpty() || name == "help") return chunk(help(caller.rank))

        val tool = DebugTools.find(name) ?: return listOf("Ferramenta desconhecida: $name. Digite debug para a lista.")
        val args = try {
            parseArgs(tool, input.substringAfter(' ', ""))
        } catch (e: DebugToolException) {
            return listOf(e.message!!)
        }

        val text = when (val o = DebugTools.run(name, args, caller, HabboServer.habboConfig.debugToolsConfig)) {
            is DebugOutcome.Ok -> DebugTools.json.writeValueAsString(o.result) + " (${o.durationMs} ms)"
            DebugOutcome.Unknown -> "Ferramenta desconhecida: $name"
            is DebugOutcome.Forbidden -> "Negado: ${o.message}"
            is DebugOutcome.BadParam -> "Erro: ${o.message}"
            is DebugOutcome.Failed -> "Falhou: ${o.message}"
        }

        return chunk(text)
    }

    private fun chunk(text: String): List<String> {
        val parts = text.chunked(CHUNK)
        if (parts.size <= MAX_CHUNKS) return parts
        return parts.take(MAX_CHUNKS) + "... (${text.length} caracteres; resposta cortada, use a aba Debug da CMS)"
    }
}
