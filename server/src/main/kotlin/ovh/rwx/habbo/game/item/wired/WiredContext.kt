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

import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomUser

data class WiredContext(
    // Itens e Usuários selecionados (Alvos definidos por Selectors)
    val targetFurnis: MutableList<RoomItem> = mutableListOf(),
    val targetUsers: MutableList<RoomEntity> = mutableListOf(),

    // Quem ou o que disparou a pilha
    val triggererUser: RoomEntity?,
    var sourceItem: RoomItem? = null,
    val trigger: WiredTrigger<*>,

    // Variáveis
    val variables: MutableMap<String, Any> = mutableMapOf(),

    // Controle de Fluxo
    var cancelled: Boolean = false,

    val batchedMovements: MutableList<WiredMoveEntry> = mutableListOf()
) {
    /**
     * Resolve em quais mobis o efeito deve ser aplicado, com base na Fonte de Origem
     * escolhida pelo usuário e salva no WiredData.
     */
    fun getEffectiveFurnis(wiredItem: WiredItem): List<RoomItem> {
        val wiredData = wiredItem.roomItem.wiredData ?: return emptyList()

        // Pega a primeira fonte configurada, ou o padrão do Wired caso não exista
        val sourceEnum = wiredData.furniSources.firstOrNull() ?: wiredItem.defaultFurniSource

        return when (sourceEnum) {
            WiredFurniSource.TRIGGERING_ITEM -> {
                // "Use o item de ativação"
                sourceItem?.let { listOf(it) } ?: emptyList()
            }

            WiredFurniSource.SELECTED_ITEMS -> {
                // "Use mobis escolhidos" (A lista clássica)
                wiredData.items.mapNotNull { wiredItem.room.itemManager.items[it] }
            }

            WiredFurniSource.SELECTOR_ITEMS -> {
                // "Usar mobis do seletor" (Preenchido na mesma tick por um Wired Selector)
                targetFurnis
            }

            WiredFurniSource.ALL_ROOM_ITEMS -> {
                // "Todos os mobis do quarto"
                wiredItem.room.itemManager.items.values.toList()
            }

            else -> emptyList() // Fallback seguro
        }
    }

    /**
     * Resolve quais usuários devem ser afetados pelo Wired, com base na Fonte de Origem
     * escolhida pelo jogador.
     */
    fun getEffectiveUsers(wiredItem: WiredItem): List<RoomEntity> {
        val wiredData = wiredItem.roomItem.wiredData ?: return emptyList()

        // Pega a primeira fonte configurada, ou o padrão do Wired caso não exista
        val sourceEnum = wiredData.userSources.firstOrNull() ?: wiredItem.defaultUserSource

        return when (sourceEnum) {
            WiredUserSource.TRIGGERING_USER -> {
                // "Use o usuário acionador" (Fonte 0)
                triggererUser?.let { listOf(it) } ?: emptyList()
            }

            WiredUserSource.SELECTOR_USERS -> {
                // "Usar usuários do seletor" (Fonte 200)
                targetUsers
            }

            WiredUserSource.ALL_ROOM_USERS -> {
                // "Todos os usuários no quarto" (Fonte 900)
                wiredItem.room.userManager.entities.values.filterIsInstance<RoomUser>().toList()
            }

            WiredUserSource.USER_BY_NAME -> {
                // "Use o Habbo especificado pelo nome" (Fonte 101)
                // Lê a string do wiredData e busca na lista de usuários do quarto
                val username = wiredData.message
                val target = wiredItem.room.userManager.entities.values.filterIsInstance<RoomUser>().find {
                    it.habboSession.userInformation.username.equals(username, ignoreCase = true)
                }
                target?.let { listOf(it) } ?: emptyList()
            }

            WiredUserSource.BOT_BY_NAME -> {
                // todo: implementar bots
                emptyList()
                /*// "Use o bot especificado pelo nome" (Fonte 100)
                val botName = wiredData.message
                // NOTA: Ajuste `isBot` ou a forma como sua base identifica Bots
                val bot = wiredItem.room.userManager.users.values.find {
                    it.isBot() && it.name.equals(botName, ignoreCase = true)
                }
                bot?.let { listOf(it) } ?: emptyList()*/
            }
            // Fontes Especiais de Colisão/Clique (10, 11) - Fallback para Acionador por enquanto
            WiredUserSource.REACHED_USER, WiredUserSource.CLICKED_USER -> {
                triggererUser?.let { listOf(it) } ?: emptyList()
            }

            else -> emptyList() // Fallback seguro
        }
    }
}