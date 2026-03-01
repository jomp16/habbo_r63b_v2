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

package ovh.rwx.habbo.game.item.wired

/**
 * Define as origens permitidas para a seleção de Usuários em um Wired.
 * Baseado no mapeamento do cliente: wiredfurni.params.sources.users.*
 */
enum class WiredUserSource(val code: Int) {
    /** * wiredfurni.params.sources.users.0
     * O usuário que causou o gatilho (pisou, falou, clicou).
     */
    TRIGGERING_USER(0),

    /** * wiredfurni.params.sources.users.10
     * Usado geralmente em gatilhos de colisão (ex: usuário alcançado por um mobi móvel).
     */
    REACHED_USER(10),

    /** * wiredfurni.params.sources.users.11
     * O usuário que foi clicado por outro jogador.
     */
    CLICKED_USER(11),

    /** * wiredfurni.params.sources.users.100
     * Lê a stringParam (textBox) e aplica o efeito no Bot que tem aquele nome.
     */
    BOT_BY_NAME(100),

    /** * wiredfurni.params.sources.users.101
     * Lê a stringParam (textBox) e aplica o efeito no Habbo que tem aquele nome.
     */
    USER_BY_NAME(101),

    /** * wiredfurni.params.sources.users.200
     * Integração com "Wired Selectors" de usuários.
     */
    SELECTOR_USERS(200),

    /** * wiredfurni.params.sources.users.201
     * Integração com "Wired Variables / Signals" de usuários.
     */
    SIGNAL_USERS(201),

    /** * wiredfurni.params.sources.users.900
     * Afeta todos os usuários presentes no quarto.
     */
    ALL_ROOM_USERS(900);

    companion object {
        fun fromCode(code: Int): WiredUserSource? = entries.find { it.code == code }
    }
}