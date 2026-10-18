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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.WiredKind
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.addon.WiredAddonType
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.variable.*
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession

abstract class WiredItem(val room: Room, val roomItem: RoomItem) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    abstract fun code(): Int

    open val requiresItems: Boolean = false
    open val requiresUsers: Boolean = false

    // As subclasses devem sobrescrever isso se quiserem habilitar opções avançadas
    open val allowedFurniSources: List<WiredFurniSource>
        get() = if (requiresItems) {
            if (this is WiredEffect) {
                listOf(
                    WiredFurniSource.SELECTED_ITEMS,
                    WiredFurniSource.SELECTOR_ITEMS
                )
            } else {
                listOf(
                    WiredFurniSource.SELECTED_ITEMS,
                    WiredFurniSource.SELECTOR_ITEMS,
                    WiredFurniSource.SIGNAL_ITEMS,
                    WiredFurniSource.TRIGGERING_ITEM,
                )
            }
        } else {
            emptyList()
        }

    open val defaultFurniSource: WiredFurniSource
        get() = if (requiresItems) WiredFurniSource.SELECTED_ITEMS else WiredFurniSource.TRIGGERING_ITEM

    open val allowedFurniSourceGroups: List<List<WiredFurniSource>>
        get() = if (allowedFurniSources.isNotEmpty()) listOf(allowedFurniSources) else emptyList()

    open val defaultFurniSourceGroups: List<WiredFurniSource>
        get() = if (allowedFurniSourceGroups.isNotEmpty()) {
            if (defaultFurniSource in (allowedFurniSourceGroups.firstOrNull() ?: emptyList())) {
                listOf(defaultFurniSource)
            } else {
                listOf(allowedFurniSourceGroups.first().first())
            }
        } else emptyList()

    open val allowedUserSources: List<WiredUserSource>
        get() = if (requiresUsers) {
            listOf(
                WiredUserSource.TRIGGERING_USER, // Usar o usuário acionador (Padrão)
                WiredUserSource.SELECTOR_USERS,  // Usar usuários do seletor
                WiredUserSource.SIGNAL_USERS     // Usar usuários do sinal
            )
        } else {
            emptyList() // Se for false, esconde o menu de opções de usuário no client
        }

    open val stuffTypeSelectionEnabled: Boolean
        get() = requiresItems

    open fun getStuffTypeSelectionCode(wiredData: WiredData): Int {
        return if (wiredData.furniSources.contains(WiredFurniSource.TRIGGERING_ITEM)) 1 else 0
    }

    open val defaultUserSource: WiredUserSource = WiredUserSource.TRIGGERING_USER

    open val allowedUserSourceGroups: List<List<WiredUserSource>>
        get() = if (allowedUserSources.isNotEmpty()) listOf(allowedUserSources) else emptyList()

    open val defaultUserSourceGroups: List<WiredUserSource>
        get() = if (allowedUserSourceGroups.isNotEmpty()) {
            if (defaultUserSource in (allowedUserSourceGroups.firstOrNull() ?: emptyList())) {
                listOf(defaultUserSource)
            } else {
                listOf(allowedUserSourceGroups.first().first())
            }
        } else emptyList()

    /**
     * Applier fino do save do diálogo Wired: o parse temporal puro vive em
     * [parseWiredSaveWire] (testável por era com bytes sintéticos); aqui fica só a
     * política de domínio — resolução de stuffIds em itens de sala válidos,
     * preservação de campos ausentes e mutação do WiredData.
     */
    fun saveWired(habboRequest: HabboRequest, habboSession: HabboSession): Boolean {
        val wiredData = roomItem.wiredData ?: return false
        val wire = parseWiredSaveWire(habboRequest, habboSession.habboVersion, roomItem.furnishing.interactionType.wiredKind)

        wiredData.message = wire.message
        wiredData.options = wire.options
        wiredData.items = wire.stuffIds.mapNotNull { itemId ->
            room.itemManager.items[itemId]?.takeIf { it.isValidWiredTarget() }?.id
        }
        wire.delay?.let { wiredData.delay = it }
        wire.quantifier?.let { log.info("[WIRED] TODO: condition quantifier: {}", it) }
        wire.filter?.let { wiredData.filter = it }
        wire.inverse?.let { wiredData.inverse = it }
        wiredData.furniSources = wire.furniSources
        wire.userSources?.let { wiredData.userSources = it }
        wire.variableIds?.let { wiredData.variableIds = it }
        wire.stuffIds2?.let { ids ->
            wiredData.stuffIds2 = ids.mapNotNull { itemId ->
                room.itemManager.items[itemId]?.takeIf { it.isValidWiredTarget() }?.id
            }
        }

        log.debug("WiredData salvo com sucesso: {}", wiredData)
        setData()
        return true
    }

    open fun setData() {
        // Override in subclasses to reload internal variables
    }

    /**
     * Applier fino do diálogo Wired (outgoing): monta [WiredDialogWireData] a partir de
     * WiredData + RoomItem + política da subclasse e delega a escrita temporal pura a
     * [writeWiredDialogWire].
     */
    fun writeDialog(
        habboResponse: HabboResponse,
        wiredData: WiredData
    ) {
        // furniSourceTypes: pega do DB. Se estiver vazio (novo), usa o default definido no WiredItem
        val furniSourceCodes =
            wiredData.furniSources.ifEmpty { defaultFurniSourceGroups }.map { it.code }
        val userSourceCodes =
            wiredData.userSources.ifEmpty { defaultUserSourceGroups }.map { it.code }

        writeWiredDialogWire(
            habboResponse,
            habboResponse.habboVersion,
            WiredDialogWireData(
                kind = roomItem.furnishing.interactionType.wiredKind,
                spriteId = roomItem.furnishing.spriteId,
                roomItemId = roomItem.id,
                requiresItems = requiresItems,
                stuffTypeSelectionEnabled = stuffTypeSelectionEnabled,
                stuffIds = wiredData.items,
                stuffIds2 = wiredData.stuffIds2,
                message = wiredData.message,
                options = wiredData.options,
                code = code(),
                delay = wiredData.delay,
                filter = wiredData.filter,
                inverse = wiredData.inverse,
                selectionCode = getStuffTypeSelectionCode(wiredData),
                variableIds = wiredData.variableIds,
                furniSourceCodes = furniSourceCodes,
                userSourceCodes = userSourceCodes,
                furniSourceGroups = allowedFurniSourceGroups,
                userSourceGroups = allowedUserSourceGroups,
                defaultFurniSourceGroups = defaultFurniSourceGroups,
                defaultUserSourceGroups = defaultUserSourceGroups,
                wiredContextBlocks = buildWiredContextBlocks(roomItem.room, wiredData, roomItem, this)
            )
        )
    }

    companion object {
        fun buildWiredContextBlocks(
            room: Room,
            wiredData: WiredData,
            roomItem: RoomItem? = null,
            wiredItem: WiredItem? = null
        ): List<WiredContextBlock> {
            val blocks = mutableListOf<WiredContextBlock>()
            // Bloco 0: Hash de todas as variáveis da sala (obrigatório para sincronização do client)
            blocks.add(WiredContextBlock.AllVariablesInRoom(room.wiredVariableManager.getAllVariablesHash()))

            for (varId in wiredData.variableIds) {
                val def = room.wiredVariableManager.getDefinition(varId) ?: continue
                when {
                    def.variableTarget == WiredVariableTarget.FURNI ||
                            def.availabilityType == VariableAvailabilityType.PERSISTENT_FURNI ||
                            def.availabilityType == VariableAvailabilityType.TEMPORARY_FURNI -> {
                        val holders = if (wiredData.items.isNotEmpty()) {
                            wiredData.items.map { itemId ->
                                val v =
                                    room.wiredVariableManager.getVariableValue(varId, itemId, VariableOwnerType.FURNI)
                                val intVal = (v?.value as? Number)?.toInt() ?: (v?.value as? String)?.toIntOrNull() ?: 0
                                ObjectIdAndValuePair(itemId, intVal)
                            }
                        } else {
                            room.itemManager.items.keys.mapNotNull { itemId ->
                                val v =
                                    room.wiredVariableManager.getVariableValue(varId, itemId, VariableOwnerType.FURNI)
                                if (v != null) {
                                    val intVal =
                                        (v.value as? Number)?.toInt() ?: (v.value as? String)?.toIntOrNull() ?: 0
                                    ObjectIdAndValuePair(itemId, intVal)
                                } else null
                            }
                        }
                        blocks.add(WiredContextBlock.FurniVariableInfoAndHolders(def, holders))
                    }

                    def.variableTarget == WiredVariableTarget.USER ||
                            def.availabilityType == VariableAvailabilityType.PERSISTENT_USER ||
                            def.availabilityType == VariableAvailabilityType.TEMPORARY_USER -> {
                        val holders =
                            room.userManager.entities.values.filterIsInstance<RoomUser>().mapNotNull { roomUser ->
                                val userId = roomUser.habboSession.userInformation.id
                                val v =
                                    room.wiredVariableManager.getVariableValue(varId, userId, VariableOwnerType.USER)
                                if (v != null) {
                                    val intVal =
                                        (v.value as? Number)?.toInt() ?: (v.value as? String)?.toIntOrNull() ?: 0
                                    ObjectIdAndValuePair(userId, intVal)
                                } else null
                            }
                        blocks.add(WiredContextBlock.UserVariableInfoAndHolders(def, holders))
                    }

                    else -> {
                        val v =
                            room.wiredVariableManager.getVariableValue(varId, room.roomData.id, VariableOwnerType.ROOM)
                        val intVal = (v?.value as? Number)?.toInt() ?: (v?.value as? String)?.toIntOrNull() ?: 0
                        blocks.add(WiredContextBlock.GlobalVariableInfoAndValue(def, intVal))
                    }
                }
            }

            val interactionType = roomItem?.furnishing?.interactionType
            val itemName = roomItem?.furnishing?.itemName ?: ""
            val interactionName = interactionType?.name ?: ""
            val itemCode = wiredItem?.code()

            // Bloco 4: SharedVariableList
            // Emitido quando o item for Addon/Variável de referência cruzada ou se houver variáveis compartilhadas de outros quartos do mesmo dono
            val isReferenceVariable = itemCode == 4 ||
                    interactionType == InteractionType.WIRED_VARIABLE_REFERENCE ||
                    interactionName.contains("VARIABLE_REFERENCE") ||
                    interactionName.contains("VAR_REFERENCE") ||
                    itemName == "wf_var_reference"

            val sharedVariables = room.wiredVariableManager.getSharedVariablesForOwner()
            if (isReferenceVariable || sharedVariables.isNotEmpty()) {
                blocks.add(WiredContextBlock.SharedVariableList(sharedVariables))
            }

            // Bloco 5: VariableList
            // Emitido quando o item for Addon de utilitário de tempo (VARIABLE_TIME_UTIL / 1002) ou manipulador de subvariáveis/tempo
            val isTimeUtil = itemCode == WiredAddonType.VARIABLE_TIME_UTIL.code ||
                    itemCode == 1002 ||
                    interactionType == InteractionType.WIRED_EXTRA_VARIABLE_TIME_UTIL ||
                    interactionName.contains("VARIABLE_TIME_UTIL") ||
                    interactionName.contains("VAR_TIME_UTIL") ||
                    itemName == "wf_xtra_var_time_util"

            if (isTimeUtil) {
                blocks.add(WiredContextBlock.VariableList(room.wiredVariableManager.getDefinitions()))
            }

            // Bloco 6: SharedGlobalPlaceholderList
            // Emitido quando o item for Addon de placeholder global (GLOBAL_PLACEHOLDER / 2000)
            val isGlobalPlaceholder = itemCode == WiredAddonType.GLOBAL_PLACEHOLDER.code ||
                    itemCode == 2000 ||
                    interactionName.contains("GLOBAL_PLACEHOLDER") ||
                    itemName.contains("global_placeholder")

            if (isGlobalPlaceholder) {
                val sharedPlaceholders = room.wiredVariableManager.getSharedGlobalPlaceholdersForOwner()
                blocks.add(WiredContextBlock.SharedGlobalPlaceholderList(sharedPlaceholders))
            }

            return blocks
        }
    }
}

/**
 * Alvo válido de wired: qualquer mobi que não seja lógica WIRED_ (a antena é a exceção
 * que conta como alvo). Consumido pelos loops de stuffIds e stuffIds2 do saveWired.
 */
private fun RoomItem.isValidWiredTarget(): Boolean =
    !(furnishing.interactionType.isWired &&
            furnishing.interactionType != InteractionType.WIRED_ANTENNA)