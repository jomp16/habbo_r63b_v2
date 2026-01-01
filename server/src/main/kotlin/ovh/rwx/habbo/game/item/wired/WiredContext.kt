/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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

import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.room.user.RoomUser

data class WiredContext(
    // Itens e Usuários selecionados (Alvos)
    val targetFurnis: MutableList<RoomItem> = mutableListOf(),
    val targetUsers: MutableList<RoomUser> = mutableListOf(),

    // Quem ou o que disparou a pilha
    val triggererUser: RoomUser? = null,
    val sourceItem: RoomItem? = null,
    val trigger: WiredTrigger? = null,

    // Variáveis (Futuro)
    // Map de Nome da Variável -> Valor (String, Int, etc.)
    val variables: MutableMap<String, Any> = mutableMapOf(),

    // Controle de Fluxo
    var cancelled: Boolean = false
) {
    // Para efeitos que mexem em mobis (como o seu de Direção)
    fun getEffectiveFurnis(wiredItem: WiredItem): List<RoomItem> {
        if (targetFurnis.isNotEmpty()) return targetFurnis

        val legacy = wiredItem.roomItem.wiredData?.items?.mapNotNull { id -> wiredItem.roomItem.room.roomItems[id] }
        if (!legacy.isNullOrEmpty()) return legacy

        return sourceItem?.let { listOf(it) } ?: emptyList()
    }

    // Para efeitos que mexem em usuários (como Teleporte ou Mensagem)
    fun getEffectiveUsers(wiredItem: WiredItem): List<RoomUser> {
        if (targetUsers.isNotEmpty()) return targetUsers

        // Fallback padrão: quem causou o evento
        return triggererUser?.let { listOf(it) } ?: emptyList()
    }
}