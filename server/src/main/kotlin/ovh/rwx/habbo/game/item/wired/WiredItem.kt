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

    fun saveWired(habboRequest: HabboRequest, habboSession: HabboSession): Boolean {
        val habboAir = habboSession.habboVersion.isAir && habboSession.habboVersion.isVersionAtLeast(2023, 4, 14)
        roomItem.wiredData?.let {
            // param2: intParams
            val intParamsCount = habboRequest.readInt()
            val options = mutableListOf<Int>()
            repeat(intParamsCount) { _ ->
                options += habboRequest.readInt()
            }

            // param4: stringParam
            it.message = habboRequest.readUTF()

            // param5: stuffIds (Mobis selecionados no quarto)
            val itemsCount = habboRequest.readInt()
            val roomItemsIds = mutableListOf<Int>()
            repeat(itemsCount) { _ ->
                val itemId = habboRequest.readInt()
                if (room.itemManager.items.containsKey(itemId)) {
                    val roomItem1 = room.itemManager.items[itemId] ?: return@repeat
                    val isWiredLogic = roomItem1.furnishing.interactionType.name.startsWith("WIRED_") &&
                            roomItem1.furnishing.interactionType != InteractionType.WIRED_ANTENNA
                    if (!isWiredLogic) {
                        roomItemsIds += itemId
                    }
                }
            }

            if (habboAir) {
                // Diferentes tipos têm diferentes parâmetros extras
                when {
                    roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT") -> {
                        // Effect: param7 = getActionDelay()
                        it.delay = habboRequest.readInt()
                    }

                    roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION") -> {
                        // Condition: param7 = resolveQuantifier()
                        val quantifier = habboRequest.readInt() // TODO: implement quantifier
                        log.info("[WIRED] TODO: condition quantifier: {}", quantifier)
                    }

                    roomItem.furnishing.interactionType.name.startsWith("WIRED_SELECTOR") -> {
                        // Selector: param7 = resolveFilterField(), param8 = resolveInverseField()
                        it.filter = habboRequest.readBoolean()
                        it.inverse = habboRequest.readBoolean()
                    }
                }

                // param7/param8: furniSourceTypes (Ex: 0 = Selected Furni, 100 = Triggering Furni)
                val furniSourcesCount = habboRequest.readInt()
                val furniSources = mutableListOf<WiredFurniSource>()
                repeat(furniSourcesCount) { _ ->
                    furniSources += WiredFurniSource.fromCode(habboRequest.readInt()) ?: return@repeat
                }
                it.furniSources = furniSources

                // param8/param9: userSourceTypes (Ex: 0 = Triggering User)
                val userSourcesCount = habboRequest.readInt()
                val userSources = mutableListOf<WiredUserSource>()
                repeat(userSourcesCount) { _ ->
                    userSources += WiredUserSource.fromCode(habboRequest.readInt()) ?: return@repeat
                }
                it.userSources = userSources

                // param3: resolveVariableIds()
                if (habboSession.habboVersion.isVersionAtLeast(2023, 6, 30)) {
                    val variableIdsCount = habboRequest.readInt()
                    val variableIds = mutableListOf<String>()
                    repeat(variableIdsCount) { _ ->
                        variableIds += if (habboSession.habboVersion.isVersionBefore(2024, 12, 12)) {
                            habboRequest.readInt().toString()
                        } else {
                            habboRequest.readUTF()
                        }
                    }
                    it.variableIds = variableIds
                }

                // param6: getStuffIds2() (Mobis secundários)
                if (habboSession.habboVersion.isVersionAtLeast(2025, 5, 6)) {
                    val stuffIds2Count = habboRequest.readInt()
                    val roomItemsIds2 = mutableListOf<Int>()
                    repeat(stuffIds2Count) { _ ->
                        val itemId = habboRequest.readInt()
                        if (room.itemManager.items.containsKey(itemId)) {
                            val roomItem2 = room.itemManager.items[itemId] ?: return@repeat
                            val isWiredLogic2 = roomItem2.furnishing.interactionType.name.startsWith("WIRED_") &&
                                    roomItem2.furnishing.interactionType != InteractionType.WIRED_ANTENNA
                            if (!isWiredLogic2) {
                                roomItemsIds2 += itemId
                            }
                        }
                    }
                    it.stuffIds2 = roomItemsIds2
                }

            } else {
                // Delay apenas para effects na versão antiga
                if (roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT")) {
                    it.delay = habboRequest.readInt()
                }

                // stuffTypeSelectionCode (Flash clássico)
                val stuffTypeSelectionCode = habboRequest.readInt()
                if (stuffTypeSelectionCode == 1) {
                    it.furniSources = listOf(WiredFurniSource.TRIGGERING_ITEM)
                } else {
                    it.furniSources = listOf(WiredFurniSource.SELECTED_ITEMS)
                }
            }

            it.options = options
            it.items = roomItemsIds
            log.debug("WiredData salvo com sucesso: {}", it)
            setData()
            return true
        }
        return false
    }

    open fun setData() {
        // Override in subclasses to reload internal variables
    }

    fun writeDialog(
        habboResponse: HabboResponse,
        wiredData: WiredData
    ) {
        val habboAir = habboResponse.isAir && habboResponse.isVersionAtLeast(2023, 4, 14)
        habboResponse.apply {
            if (!habboAir && isVersionBetween(2011, 2, 22, 2023, 4, 14)) {
                writeBoolean(stuffTypeSelectionEnabled)
            }

            // Determine if this wired uses items
            if (requiresItems) {
                writeItems(wiredData, habboAir)
            } else {
                writeEmptyItems(habboAir)
            }

            writeItemInfo(roomItem)

            // Determine settings based on wired data
            val settingsCount = when {
                wiredData.message.isNotEmpty() && wiredData.options.isNotEmpty() -> wiredData.options.size
                wiredData.message.isNotEmpty() -> 0
                wiredData.options.isNotEmpty() -> wiredData.options.size
                else -> 0
            }

            writeSettings(
                wiredData.message,
                wiredData.options,
                settingsCount,
                habboAir,
                getStuffTypeSelectionCode(wiredData)
            )

            if (habboAir) {
                if (isVersionAtLeast(2023, 6, 30)) {
                    writeInt(wiredData.variableIds.size)
                    wiredData.variableIds.forEach {
                        if (isVersionBefore(2024, 12, 12)) {
                            writeInt(it.toIntOrNull() ?: 0)
                        } else {
                            writeUTF(it)
                        }
                    }
                }
                // furniSourceTypes: Pega do DB. Se estiver vazio (novo), usa o default definido no WiredItem
                val fSources =
                    wiredData.furniSources.ifEmpty { this@WiredItem.defaultFurniSourceGroups }.map { it.code }
                writeListOfIds(fSources)

                // userSourceTypes: Pega do DB. Se estiver vazio (novo), usa o default definido no WiredItem
                val uSources =
                    wiredData.userSources.ifEmpty { this@WiredItem.defaultUserSourceGroups }.map { it.code }
                writeListOfIds(uSources)
            }

            // Para o Habbo antes do AIR , a classe pai termina antes de CODE
            writeInt(code())

            if (roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT")) {
                writeDelay(wiredData.delay)
            } else if (habboAir && roomItem.furnishing.interactionType.name.startsWith("WIRED_SELECTOR")) {
                writeSelectorDefinitionSpecifics(wiredData.filter, wiredData.inverse)
            } else if (habboAir && roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION")) {
                if (isVersionAtLeast(2024, 5, 6)) {
                    writeConditionDefinitionSpecifics(quantifierCode = 0)
                } else {
                    writeConditionDefinitionSpecifics(quantifierCode = 0)
                    writeBoolean(false) // isInvert
                }
            }

            if (habboAir) {
                // A aba "Avançado" só deve aparecer se o Wired tiver opções de fonte configuradas
                val hasAdvancedMode = this@WiredItem.allowedFurniSourceGroups.sumOf { it.size } > 1 ||
                        this@WiredItem.allowedUserSourceGroups.sumOf { it.size } > 1 ||
                        this@WiredItem.allowedFurniSourceGroups.size > 1 ||
                        this@WiredItem.allowedUserSourceGroups.size > 1
                writeBoolean(hasAdvancedMode)
                writeInputSourcesConf(this@WiredItem)
                writeBoolean(false) // allowWallFurni

                if (roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION")) {
                    if (isVersionAtLeast(2024, 5, 6)) {
                        writeConditionTypeSpecifics(quantifierType = 0, isInvert = false)
                    } else {
                        writeByte(0) // quantifierType lido no final do ctor da subclasse em 2023
                    }
                }

                if (isVersionAtLeast(2024, 5, 6)) {
                    writeWiredContext(buildWiredContextBlocks(roomItem.room, wiredData, roomItem, this@WiredItem))
                }
                if (isVersionAtLeast(2025, 5, 6)) {
                    writeDefaultIntParams()
                }
            } else {
                // Legacy format - conflictingActions
                if (roomItem.furnishing.interactionType.name.startsWith("WIRED_TRIGGER")) {
                    writeBlockedTriggers(wiredData)
                } else if (roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT")) {
                    writeBlockedActions(wiredData)
                }
            }
        }
    }

    companion object {
        fun HabboResponse.writeEmptyItems(habboAir: Boolean) {
            writeInt(0) // selectable items
            writeInt(0) // items

            if (habboAir && isVersionAtLeast(2025, 5, 6)) {
                writeInt(0) // stuffIds2
            }
        }

        fun HabboResponse.writeItems(wiredData: WiredData, habboAir: Boolean) {
            writeInt(20) // selectable items
            writeInt(wiredData.items.size)

            wiredData.items.forEach {
                writeInt(it)
            }

            if (habboAir && isVersionAtLeast(2025, 5, 6)) {
                writeInt(wiredData.stuffIds2.size)
                wiredData.stuffIds2.forEach {
                    writeInt(it)
                }
            }
        }

        fun HabboResponse.writeItemInfo(roomItem: RoomItem) {
            writeInt(roomItem.furnishing.spriteId)
            writeInt(roomItem.id)
        }

        fun HabboResponse.writeDelay(delay: Int) {
            writeInt(delay)
        }

        @Suppress("UNUSED_PARAMETER")
        fun HabboResponse.writeBlockedTriggers(wiredData: WiredData) {
            // todo
            /*val blockedTriggers = mutableSetOf<Int>()

            writeInt(blockedTriggers.size)
            blockedTriggers.forEach { writeInt(it) }*/

            writeInt(0)
        }

        @Suppress("UNUSED_PARAMETER")
        fun HabboResponse.writeBlockedActions(wiredData: WiredData) {
            // todo
            writeInt(0)
        }

        fun HabboResponse.writeSettings(
            text: String,
            options: List<Int>,
            settingsCount: Int,
            habboAir: Boolean,
            selectionCode: Int = 0
        ) {
            writeUTF(text)
            writeInt(settingsCount)
            if (options.isNotEmpty()) {
                options.forEach {
                    writeInt(it)
                }
            }
            if (!habboAir) {
                writeInt(selectionCode) // stuffTypeSelectionCode no Flash clássico
            }
        }

        fun HabboResponse.writeListOfIds(list: List<Int>) {
            writeInt(list.size)
            list.forEach {
                writeInt(it)
            }
        }

        /**
         * Constrói dinamicamente os menus dropdown de origem do Wired baseado
         * nas permissões definidas na subclasse (allowedFurniSourceGroups / allowedUserSourceGroups).
         */
        fun HabboResponse.writeInputSourcesConf(wiredItem: WiredItem) {
            val furniGroups = wiredItem.allowedFurniSourceGroups
            val userGroups = wiredItem.allowedUserSourceGroups
            val defaultFurni = wiredItem.defaultFurniSourceGroups
            val defaultUser = wiredItem.defaultUserSourceGroups

            // 1. _SafeStr_7491 (getAllowedFurniSources)
            writeInt(furniGroups.size)
            furniGroups.forEach { group ->
                writeInt(group.size)
                group.forEach { writeInt(it.code) }
            }

            // 2. _SafeStr_7492 (getAllowedUserSources)
            writeInt(userGroups.size)
            userGroups.forEach { group ->
                writeInt(group.size)
                group.forEach { writeInt(it.code) }
            }

            // 3. _SafeStr_7493 (defaultFurniSources)
            writeInt(defaultFurni.size)
            defaultFurni.forEach { writeInt(it.code) }

            // 4. _SafeStr_7494 (defaultUserSources)
            writeInt(defaultUser.size)
            defaultUser.forEach { writeInt(it.code) }
        }

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

        fun HabboResponse.writeWiredContext(blocks: List<WiredContextBlock>) {
            writeInt(blocks.size)
            blocks.forEach { serialize(it) }
        }

        fun HabboResponse.writeDefaultIntParams() {
            writeInt(0) // size
            // writeInt(0) // todo param
        }

        fun HabboResponse.writeSelectorDefinitionSpecifics(isFilter: Boolean, isInvert: Boolean) {
            writeBoolean(isFilter)
            writeBoolean(isInvert)
        }

        fun HabboResponse.writeConditionDefinitionSpecifics(quantifierCode: Int) {
            writeInt(quantifierCode)
        }

        fun HabboResponse.writeConditionTypeSpecifics(quantifierType: Byte, isInvert: Boolean) {
            writeByte(quantifierType.toInt())
            writeBoolean(isInvert)
        }
    }
}