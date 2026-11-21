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

package ovh.rwx.habbo.config

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Ferramentas de debug remoto ([ovh.rwx.habbo.admin.debug.DebugTools]): MCP do webapp, aba da CMS e `debug` do
 * Server Console. Ferramentas de leitura pedem [readMinRank]; as que mudam estado, [writeMinRank]; o `eval` também
 * precisa de [evalEnabled].
 */
data class DebugToolsConfig(
    @param:JsonProperty("read_min_rank")
    val readMinRank: Int = 7,
    @param:JsonProperty("write_min_rank")
    val writeMinRank: Int = 7,
    /** Liga o `eval` (script JS com acesso total à JVM). Deixe desligado fora de uma sessão de debug. */
    @param:JsonProperty("eval_enabled")
    val evalEnabled: Boolean = false,
    /** Tokens de acesso do endpoint MCP; vazio = só o JWT admin da CMS. */
    @param:JsonProperty("tokens")
    val tokens: List<DebugToken> = emptyList(),
)

/**
 * Token dedicado de debug: só o sha256 (hex) do token fica no config. O rank vem do banco a cada uso (rebaixar o
 * usuário revoga o acesso); apagar a entrada e reiniciar revoga o token.
 */
data class DebugToken(
    @param:JsonProperty("name")
    val name: String = "",
    @param:JsonProperty("user_id", required = true)
    val userId: Int,
    @param:JsonProperty("sha256", required = true)
    val sha256: String,
)
