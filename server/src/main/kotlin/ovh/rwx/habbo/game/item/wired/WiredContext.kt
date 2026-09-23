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
import ovh.rwx.habbo.game.item.wired.variable.InternalVariableDefinition
import ovh.rwx.habbo.game.item.wired.variable.VariableOwnerType
import ovh.rwx.habbo.game.item.wired.variable.WiredVariableTarget
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.room.wired.WiredVariableManager

data class WiredContext(
    // Itens e Usuários selecionados (Alvos definidos por Selectors)
    val targetFurnis: MutableList<RoomItem> = mutableListOf(),
    val targetUsers: MutableList<RoomEntity> = mutableListOf(),

    // Itens e Usuários recebidos através de Sinais / Antenas (Wired 2.0 Signals)
    val signalFurnis: MutableList<RoomItem> = mutableListOf(),
    val signalUsers: MutableList<RoomEntity> = mutableListOf(),

    // Quem ou o que disparou a pilha
    val triggererUser: RoomEntity?,
    var sourceItem: RoomItem? = null,
    val trigger: WiredTrigger<*>,

    // Variáveis
    val variableManager: WiredVariableManager = trigger.room.wiredVariableManager,
    val variables: MutableMap<String, Any> = mutableMapOf(),
    val placeholders: MutableMap<String, String> = mutableMapOf(),

    // Controle de Fluxo
    var cancelled: Boolean = false,
    val executionDepth: Int = 0,

    val batchedMovements: MutableList<WiredMoveEntry> = mutableListOf()
) {
    fun getVariable(
        variableId: String,
        ownerId: Int = trigger.room.roomData.id,
        ownerType: VariableOwnerType = VariableOwnerType.ROOM
    ): Any? {
        return variables[variableId] ?: variableManager.getVariableValue(variableId, ownerId, ownerType)?.value
    }

    fun setVariable(
        variableId: String,
        value: Any,
        ownerId: Int = trigger.room.roomData.id,
        ownerType: VariableOwnerType = VariableOwnerType.ROOM
    ) {
        variables[variableId] = value
        variableManager.setVariableValue(variableId, value, ownerId, ownerType)
    }

    fun formatPlaceholders(input: String): String {
        var result = input
        val user = (triggererUser as? RoomUser)
        if (user != null) {
            result = result.replace("%username%", user.habboSession.userInformation.username, ignoreCase = true)
            result = result.replace("%user%", user.habboSession.userInformation.username, ignoreCase = true)
            result = result.replace("%userid%", user.habboSession.userInformation.id.toString(), ignoreCase = true)
        }
        if (targetUsers.isNotEmpty()) {
            val targetUsernames = targetUsers.mapNotNull { (it as? RoomUser)?.habboSession?.userInformation?.username }
            if (targetUsernames.isNotEmpty()) {
                result = result.replace("%target_user%", targetUsernames.joinToString(", "), ignoreCase = true)
            }
        }
        sourceItem?.let {
            result = result.replace("%furniname%", it.furnishing.itemName, ignoreCase = true)
            result = result.replace("%furni%", it.furnishing.itemName, ignoreCase = true)
        }
        if (targetFurnis.isNotEmpty()) {
            val furniNames = targetFurnis.map { it.furnishing.itemName }
            result = result.replace("%target_furni%", furniNames.joinToString(", "), ignoreCase = true)
        }
        placeholders.forEach { (key, value) ->
            result = result.replace("%$key%", value, true)
            result = result.replace($$"$$$key", value, true)
            result = result.replace($$"$($$key)", value, ignoreCase = true)
            result = result.replace("#$key", value, ignoreCase = true)
            result = result.replace("#($key)", value, ignoreCase = true)
        }
        variables.forEach { (key, value) ->
            result = result.replace("%var.$key%", value.toString(), ignoreCase = true)
            result = result.replace("%variable.$key%", value.toString(), ignoreCase = true)
            result = result.replace($$"$var.$$key", value.toString(), ignoreCase = true)
            result = result.replace($$"$$$key", value.toString(), ignoreCase = true)
            result = result.replace($$"$($$key)", value.toString(), ignoreCase = true)
            result = result.replace("#$key", value.toString(), ignoreCase = true)
            result = result.replace("#($key)", value.toString(), ignoreCase = true)
        }
        return result
    }

    /**
     * Resolve em quais mobis o efeito deve ser aplicado, com base na Fonte de Origem
     * escolhida pelo usuário e salva no WiredData.
     */
    fun getEffectiveFurnis(wiredItem: WiredItem, groupIndex: Int = 0): List<RoomItem> {
        val wiredData = wiredItem.roomItem.wiredData ?: return emptyList()

        val sourceEnum = wiredData.furniSources.getOrNull(groupIndex)
            ?: wiredItem.defaultFurniSourceGroups.getOrNull(groupIndex)
            ?: wiredItem.defaultFurniSource

        return when (sourceEnum) {
            WiredFurniSource.TRIGGERING_ITEM -> {
                // "Use o item de ativação"
                sourceItem?.let { listOf(it) }
                    ?: targetFurnis.ifEmpty { wiredData.items.mapNotNull { wiredItem.room.itemManager.items[it] } }
            }

            WiredFurniSource.SELECTED_ITEMS -> {
                // "Use mobis escolhidos"
                if (groupIndex == 1 && wiredData.stuffIds2.isNotEmpty()) {
                    wiredData.stuffIds2.mapNotNull { wiredItem.room.itemManager.items[it] }
                } else {
                    wiredData.items.mapNotNull { wiredItem.room.itemManager.items[it] }
                }
            }

            WiredFurniSource.SELECTOR_ITEMS -> {
                // "Usar mobis do seletor"
                targetFurnis
            }

            WiredFurniSource.SIGNAL_ITEMS -> {
                // "Usar mobis do sinal"
                signalFurnis.ifEmpty { targetFurnis }
            }

            WiredFurniSource.ALL_ROOM_ITEMS -> {
                // "Todos os mobis do quarto"
                wiredItem.room.itemManager.items.values.toList()
            }

            else -> emptyList()
        }
    }

    /**
     * Resolve quais usuários devem ser afetados pelo Wired, com base na Fonte de Origem
     * escolhida pelo jogador.
     */
    fun getEffectiveUsers(wiredItem: WiredItem, groupIndex: Int = 0): List<RoomEntity> {
        val wiredData = wiredItem.roomItem.wiredData ?: return emptyList()

        val sourceEnum = wiredData.userSources.getOrNull(groupIndex)
            ?: wiredItem.defaultUserSourceGroups.getOrNull(groupIndex)
            ?: wiredItem.defaultUserSource

        return when (sourceEnum) {
            WiredUserSource.TRIGGERING_USER -> {
                // "Use o usuário acionador" (Fonte 0)
                triggererUser?.let { listOf(it) } ?: emptyList()
            }

            WiredUserSource.SELECTOR_USERS -> {
                // "Usar usuários do seletor" (Fonte 200)
                targetUsers
            }

            WiredUserSource.SIGNAL_USERS -> {
                // "Usar usuários do sinal" (Fonte 201)
                signalUsers.ifEmpty { targetUsers.ifEmpty { triggererUser?.let { listOf(it) } ?: emptyList() } }
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
        }
    }

    fun resolveContextInternalVariable(variableId: String): Long? {
        val def = InternalVariableDefinition.find(variableId) ?: return null
        return when (def) {
            InternalVariableDefinition.CONTEXT_SELECTOR_FURNI_COUNT -> targetFurnis.size.toLong()
            InternalVariableDefinition.CONTEXT_SELECTOR_USER_COUNT -> targetUsers.size.toLong()
            InternalVariableDefinition.CONTEXT_SIGNAL_FURNI_COUNT -> signalFurnis.size.toLong()
            InternalVariableDefinition.CONTEXT_ANTENNA_ID -> 0L
            InternalVariableDefinition.CONTEXT_CHAT_TYPE -> 0L
            InternalVariableDefinition.CONTEXT_CHAT_STYLE -> 0L
            else -> null
        }
    }

    fun resolveVariableValue(
        target: WiredVariableTarget,
        variableId: String,
        wiredItem: WiredItem,
        groupIndex: Int = 0
    ): Long {
        return when (target) {
            WiredVariableTarget.CONTEXT -> {
                val contextVal = resolveContextInternalVariable(variableId)
                if (contextVal != null) return contextVal

                val v = variables[variableId]
                (v as? Number)?.toLong() ?: (v as? String)?.toLongOrNull() ?: 0L
            }

            WiredVariableTarget.GLOBAL -> {
                val v = variableManager.getVariableValue(variableId, trigger.room.roomData.id, VariableOwnerType.ROOM)
                (v?.value as? Number)?.toLong() ?: (v?.value as? String)?.toLongOrNull() ?: 0L
            }

            WiredVariableTarget.FURNI -> {
                val furnis = getEffectiveFurnis(wiredItem, groupIndex)
                val furni = furnis.firstOrNull() ?: return 0L
                val v = variableManager.getVariableValue(variableId, furni.id, VariableOwnerType.FURNI)
                (v?.value as? Number)?.toLong() ?: (v?.value as? String)?.toLongOrNull() ?: 0L
            }

            WiredVariableTarget.USER -> {
                val users = getEffectiveUsers(wiredItem, groupIndex)
                val user = users.firstOrNull() ?: return 0L
                val userId = (user as? RoomUser)?.habboSession?.userInformation?.id ?: user.virtualID
                val v = variableManager.getVariableValue(variableId, userId, VariableOwnerType.USER)
                (v?.value as? Number)?.toLong() ?: (v?.value as? String)?.toLongOrNull() ?: 0L
            }
        }
    }
}