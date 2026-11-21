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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.config.DebugToken
import ovh.rwx.habbo.config.DebugToolsConfig
import ovh.rwx.habbo.game.room.IRoomTask
import ovh.rwx.habbo.game.room.Room
import java.security.MessageDigest
import java.util.HexFormat
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/** Tipo de um parâmetro (o nome é o tipo JSON Schema do MCP). */
enum class DebugParamType(val jsonType: String) { STRING("string"), INT("integer"), BOOL("boolean") }

data class DebugParam(val name: String, val type: DebugParamType, val description: String, val required: Boolean = false)

/** Nível de acesso: leitura, mudança de estado, ou eval (que ainda pede `debug_tools.eval_enabled`). */
enum class DebugAccess { READ, WRITE, EVAL }

class DebugTool(
    val name: String,
    val description: String,
    val access: DebugAccess,
    val params: List<DebugParam> = emptyList(),
    val handler: (DebugArgs) -> Any?,
)

/** Quem chamou (para rank e auditoria); [via] = `mcp`, `cms` ou `console`. */
data class DebugCaller(val userId: Int, val username: String, val rank: Int, val via: String) {
    override fun toString() = "$userId/$username via $via"
}

/** Parâmetro inválido ou estado que impede a ferramenta: vira [DebugOutcome.BadParam] (mensagem segura para o cliente). */
class DebugToolException(message: String) : RuntimeException(message)

sealed interface DebugOutcome {
    data class Ok(val tool: String, val durationMs: Long, val result: Any?) : DebugOutcome
    data object Unknown : DebugOutcome
    data class Forbidden(val message: String) : DebugOutcome
    data class BadParam(val message: String) : DebugOutcome
    data class Failed(val message: String) : DebugOutcome
}

/** Argumentos já validados contra os [DebugParam]s da ferramenta. */
class DebugArgs(private val values: Map<String, Any?>) {
    fun string(name: String): String? = values[name] as String?
    fun int(name: String): Int? = values[name] as Int?
    fun bool(name: String, default: Boolean = false): Boolean = values[name] as Boolean? ?: default
    fun requireInt(name: String): Int = int(name) ?: throw DebugToolException("parâmetro '$name' é obrigatório")
    fun requireString(name: String): String = string(name) ?: throw DebugToolException("parâmetro '$name' é obrigatório")
}

/**
 * Ferramentas de debug remoto do emulador (catálogo em [DebugToolset]). Um só núcleo para o endpoint MCP, a aba Debug
 * da CMS e o `debug` do Server Console: validação de parâmetros, rank por nível de acesso e log de auditoria
 * (logger `ovh.rwx.habbo.admin`) ficam aqui, nunca nas portas.
 */
object DebugTools {
    const val MAX_STRING_LENGTH = 20_000

    private val audit = LoggerFactory.getLogger("ovh.rwx.habbo.admin")

    /** Serializa os resultados (mapas, listas e data classes; datas já vêm como texto). */
    val json: ObjectMapper = ObjectMapper().registerKotlinModule()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

    val tools: List<DebugTool> get() = DebugToolset.tools

    fun find(name: String): DebugTool? = tools.firstOrNull { it.name == name }

    /** null = pode usar; senão, o motivo. */
    fun denyReason(tool: DebugTool, rank: Int, config: DebugToolsConfig): String? = when (tool.access) {
        DebugAccess.READ -> if (rank < config.readMinRank) "rank insuficiente (precisa ${config.readMinRank})" else null
        DebugAccess.WRITE -> if (rank < config.writeMinRank) "rank insuficiente (precisa ${config.writeMinRank})" else null
        DebugAccess.EVAL -> when {
            !config.evalEnabled -> "eval desligado (debug_tools.eval_enabled: false)"
            rank < config.writeMinRank -> "rank insuficiente (precisa ${config.writeMinRank})"
            else -> null
        }
    }

    /** Ferramentas que o rank enxerga (o eval desligado some da lista). */
    fun visibleTo(rank: Int, config: DebugToolsConfig): List<DebugTool> = tools.filter { denyReason(it, rank, config) == null }

    /** Converte e valida os argumentos crus (JSON ou texto do console). Lança [DebugToolException]. */
    fun coerceArgs(tool: DebugTool, raw: Map<String, Any?>): DebugArgs {
        val byName = tool.params.associateBy { it.name }
        raw.keys.firstOrNull { it !in byName }?.let { throw DebugToolException("parâmetro desconhecido '$it'") }

        val values = HashMap<String, Any?>()
        for (p in tool.params) {
            val v = raw[p.name]
            if (v == null) {
                if (p.required) throw DebugToolException("parâmetro '${p.name}' é obrigatório")
                continue
            }
            values[p.name] = when (p.type) {
                DebugParamType.INT -> when (v) {
                    is Int -> v
                    is Number -> v.toLong().takeIf { it.toDouble() == v.toDouble() && it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
                    is String -> v.trim().toIntOrNull()
                    else -> null
                } ?: throw DebugToolException("parâmetro '${p.name}' deve ser inteiro")
                DebugParamType.BOOL -> when (v) {
                    is Boolean -> v
                    is String -> when (v.trim().lowercase()) {
                        "true", "1", "yes", "sim" -> true
                        "false", "0", "no", "nao", "não" -> false
                        else -> null
                    }
                    else -> null
                } ?: throw DebugToolException("parâmetro '${p.name}' deve ser booleano")
                DebugParamType.STRING -> (v as? String ?: v.toString()).also {
                    if (it.length > MAX_STRING_LENGTH) throw DebugToolException("parâmetro '${p.name}' longo demais")
                }
            }
        }

        return DebugArgs(values)
    }

    /** Executa [name] em nome de [caller] na thread do chamador. Nunca devolve stack trace. */
    fun run(name: String, raw: Map<String, Any?>, caller: DebugCaller, config: DebugToolsConfig): DebugOutcome {
        val tool = find(name) ?: return DebugOutcome.Unknown

        denyReason(tool, caller.rank, config)?.let {
            audit.warn("debug-tool actor={} tool={} result=forbidden reason={}", caller, name, it)
            return DebugOutcome.Forbidden(it)
        }

        val args = try {
            coerceArgs(tool, raw)
        } catch (e: DebugToolException) {
            return DebugOutcome.BadParam(e.message!!)
        }

        val argsLog = raw.entries.joinToString(", ") { (k, v) -> "$k=${v.toString().take(2000)}" }
        val start = System.nanoTime()

        return try {
            val result = tool.handler(args)
            val ms = (System.nanoTime() - start) / 1_000_000
            audit.info("debug-tool actor={} tool={} args=[{}] duration_ms={} result=ok", caller, name, argsLog, ms)
            DebugOutcome.Ok(name, ms, result)
        } catch (e: DebugToolException) {
            audit.info("debug-tool actor={} tool={} args=[{}] result=refused reason={}", caller, name, argsLog, e.message)
            DebugOutcome.BadParam(e.message!!)
        } catch (e: Throwable) {
            audit.error("debug-tool actor={} tool={} args=[{}] result=error", caller, name, argsLog, e)
            DebugOutcome.Failed("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    // region Tokens

    fun sha256Hex(token: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)))

    /** O token dedicado que bate com [token] (comparação em tempo constante), ou null. */
    fun findToken(token: String, config: DebugToolsConfig): DebugToken? {
        if (token.isBlank()) return null
        val digest = sha256Hex(token).toByteArray()
        var found: DebugToken? = null
        for (t in config.tokens) {
            if (MessageDigest.isEqual(digest, t.sha256.trim().lowercase().toByteArray())) found = t
        }
        return found
    }

    // endregion

    // region Room thread

    /**
     * Roda [block] na thread da sala (fila de alta frequência) e espera até [timeoutMs]. Com o loop parado, ou se ele
     * não pegar a tarefa a tempo (travado), só roda direto daqui com [force]; a tarefa nunca roda duas vezes.
     */
    fun <T> onRoomThread(room: Room, force: Boolean, timeoutMs: Long = 3000, block: (Room) -> T): T {
        if (!room.running) {
            if (!force) throw DebugToolException("loop da sala ${room.roomData.id} parado (${room.loopState}); repita com force=true para rodar fora da thread da sala")
            return block(room)
        }

        val claimed = AtomicBoolean(false)
        val future = CompletableFuture<T>()

        room.addTask(object : IRoomTask {
            override val highFrequency: Boolean get() = true

            override fun executeTask(room: Room) {
                if (!claimed.compareAndSet(false, true)) return
                try {
                    future.complete(block(room))
                } catch (e: Throwable) {
                    future.completeExceptionally(e)
                }
            }
        })

        try {
            return await(future, timeoutMs)
        } catch (e: TimeoutException) {
            if (!force) throw DebugToolException("a sala ${room.roomData.id} não rodou a tarefa em ${timeoutMs} ms (loop travado?); repita com force=true")
            if (claimed.compareAndSet(false, true)) return block(room)
        }

        // a sala pegou a tarefa bem no limite: espera ela terminar
        return try {
            await(future, timeoutMs)
        } catch (e: TimeoutException) {
            throw DebugToolException("a tarefa começou na thread da sala ${room.roomData.id} mas não terminou em ${timeoutMs} ms")
        }
    }

    private fun <T> await(future: CompletableFuture<T>, timeoutMs: Long): T = try {
        future.get(timeoutMs, TimeUnit.MILLISECONDS)
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    }

    // endregion
}
