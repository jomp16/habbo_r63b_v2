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

package ovh.rwx.habbo.game.gamedata

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.kotlin.urlUserAgent

/**
 * O `external_variables` que os clients recebem: fonte única de furnidata, figuredata, Habbicons e reciclador.
 * Lido no boot por [loadOrFail] (o servidor não sobe sem ele) e relido por [reload].
 *
 * A URL é `external_variables_txt` do `config.yaml` e precisa ser a MESMA que os clients usam, já com o override
 * (`external_variables.php` mescla `override_rwx/`): senão o servidor lê valores diferentes dos do client.
 *
 * Limite: o valor é global. O client lê um `external_variables` por release; se releases diferentes passarem a
 * receber variables diferentes, a leitura precisa ser por URL/release.
 */
object ExternalVariables {
    private val log = LoggerFactory.getLogger(javaClass)

    private const val TIMEOUT_MS = 15_000

    const val FURNIDATA = "furnidata.load.url"
    const val FIGUREDATA = "external.figurepartlist.txt"
    const val HABBICONS_HASH = "habbicons.hash"
    const val HABBICONS_URL = "habbicons.url"
    const val RECYCLER_SLOTS = "recycler.number_of_slots"

    /** Nome lido só pela `RELEASE63-201204162302` (as seguintes já leem [RECYCLER_SLOTS]). */
    const val RECYCLER_SLOTS_LEGACY = "catalog.recycler.number.of.slots"

    /**
     * Uma exigência: ao menos uma de [keys] presente, não vazia e aceita por [valid]. [description] aparece no erro.
     */
    class Requirement(val keys: List<String>, val description: String, val valid: (String) -> Boolean = { true })

    /** Toda chave que o servidor lê; sem uma delas o conteúdo é recusado (boot falha, reload mantém o anterior). */
    val REQUIREMENTS: List<Requirement> = listOf(
        Requirement(listOf(FURNIDATA), FURNIDATA),
        Requirement(listOf(FIGUREDATA), FIGUREDATA),
        Requirement(listOf(HABBICONS_HASH), HABBICONS_HASH),
        Requirement(listOf(HABBICONS_URL), HABBICONS_URL),
        Requirement(
            listOf(RECYCLER_SLOTS, RECYCLER_SLOTS_LEGACY),
            "$RECYCLER_SLOTS (or $RECYCLER_SLOTS_LEGACY) as a positive integer"
        ) { (it.toIntOrNull() ?: 0) > 0 },
    )

    @Volatile
    private var values: Map<String, String>? = null

    /**
     * Leitura do boot: baixa, valida e publica, ou lança (o servidor não sobe sem as variables).
     * Bloqueia (rede).
     */
    fun loadOrFail() {
        val (url, parsed) = fetchValidated()

        values = parsed
        log.info("Loaded {} external variables from {}", parsed.size, url)
    }

    /**
     * Relê; se baixar falhar ou faltar alguma chave, mantém o conteúdo atual e devolve `false`.
     * Bloqueia (rede): nunca em thread do Netty.
     */
    fun reload(): Boolean = runCatching { loadOrFail() }
        .onFailure { log.warn("Could not reload external variables (keeping the current ones)", it) }
        .isSuccess

    private fun fetchValidated(): Pair<String, Map<String, String>> {
        val url = HabboServer.habboConfig.externalVariablesTxt
        check(url.isNotBlank()) { "external_variables_txt is blank in config.yaml" }

        val connection = urlUserAgent(url).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        val parsed = try {
            connection.inputStream.bufferedReader().useLines { parse(it) }
        } catch (e: Exception) {
            throw IllegalStateException("Could not read external variables from $url", e)
        }

        val problems = validate(parsed)
        check(problems.isEmpty()) { "External variables from $url are missing or invalid: ${problems.joinToString()}" }

        return url to parsed
    }

    /** O que falta ou está inválido em [variables] (vazio = ok), pela descrição de cada [REQUIREMENTS]. */
    fun validate(variables: Map<String, String>): List<String> = REQUIREMENTS
        .filterNot { requirement ->
            requirement.keys.any { key -> variables[key]?.takeIf { it.isNotBlank() }?.let(requirement.valid) == true }
        }
        .map { it.description }

    /** Conteúdo atual (imutável). Só é `null` antes do boot ([loadOrFail]). */
    fun snapshot(): Map<String, String>? = values

    /** Valor de [key]; lança se as variables não foram lidas ou a chave falta (as de [REQUIREMENTS] nunca faltam). */
    fun require(key: String): String {
        val current = values ?: error("external variables were not loaded ($key)")

        return current[key]?.takeIf { it.isNotBlank() } ?: error("$key was not found in external variables")
    }

    /**
     * Formato `chave=valor`, uma por linha. Chave e valor sem espaços nas pontas (inclui o `\r` do CRLF); o valor
     * pode conter `=`. Linhas sem `=`, com chave vazia ou iniciadas por `#` são ignoradas; chave repetida: vale a
     * última, como no override.
     */
    fun parse(lines: Sequence<String>): Map<String, String> {
        val result = LinkedHashMap<String, String>()

        lines.forEach { raw ->
            val line = raw.trim()
            if (line.startsWith('#')) return@forEach

            val separator = line.indexOf('=')
            if (separator <= 0) return@forEach

            val key = line.substring(0, separator).trim()
            if (key.isEmpty()) return@forEach

            result[key] = line.substring(separator + 1).trim()
        }

        return result
    }
}
