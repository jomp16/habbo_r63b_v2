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

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.HabboVersion
import ovh.rwx.habbo.game.item.WiredKind
import ovh.rwx.habbo.game.item.wired.variable.WiredContextBlock

/**
 * Wire puro do diálogo Wired (save incoming + dialog outgoing), espelho do mesmo wire
 * em duas direções. As datas de era moram em [WiredDialogWire] — a ÚNICA fonte; este
 * módulo só decide ordem e forma. Puro: bytes ⇄ DTO, sem Room/RoomItem/Netty vivo
 * (padrão RoomSettingsWire — teste de matriz de eras em WiredDialogWireTest).
 *
 * Campos null do [WiredSaveWire] = a compilação cliente da era não envia; o applier
 * (WiredItem.saveWired) preserva o estado existente no WiredData (GLOSSARY:
 * "Preservação de Estado em Campos Opcionais Ausentes").
 */
data class WiredSaveWire(
    val options: List<Int>,
    val message: String,
    /** stuffIds brutos — o applier resolve em itens de sala válidos (isValidWiredTarget). */
    val stuffIds: List<Int>,
    /** Só EFFECT: delay (AIR e legado). */
    val delay: Int?,
    /** Só CONDITION AIR: quantifier — drenado, ainda não implementado. */
    val quantifier: Int?,
    /** Só SELECTOR AIR. */
    val filter: Boolean?,
    val inverse: Boolean?,
    val furniSources: List<WiredFurniSource>,
    /** null = era não envia (legado preserva o estado existente). */
    val userSources: List<WiredUserSource>?,
    /** null = era sem a seção (nasce 2024-05-06, junto com o WiredContext). */
    val variableIds: List<String>?,
    /** null = era sem a seção (2025-05-06). */
    val stuffIds2: List<Int>?,
)

fun parseWiredSaveWire(habboRequest: HabboRequest, version: HabboVersion, kind: WiredKind?): WiredSaveWire {
    val wire = WiredDialogWire(version)
    val habboAir = wire.isAirDialog

    // param2: intParams
    val intParamsCount = habboRequest.readInt()
    val options = mutableListOf<Int>()
    repeat(intParamsCount) { _ ->
        options += habboRequest.readInt()
    }

    // param4: stringParam
    val message = habboRequest.readUTF()

    // param5: stuffIds (mobis selecionados no quarto)
    val stuffIds = mutableListOf<Int>()
    val itemsCount = habboRequest.readInt()
    repeat(itemsCount) { _ ->
        stuffIds += habboRequest.readInt()
    }

    val delay: Int?
    val quantifier: Int?
    val filter: Boolean?
    val inverse: Boolean?
    val furniSources: List<WiredFurniSource>
    val userSources: List<WiredUserSource>?
    val variableIds: List<String>?
    val stuffIds2: List<Int>?

    if (habboAir) {
        // Diferentes tipos têm diferentes parâmetros extras
        when (kind) {
            WiredKind.EFFECT -> delay = habboRequest.readInt() // param7 = getActionDelay()
            else -> delay = null
        }
        quantifier = when (kind) {
            WiredKind.CONDITION -> habboRequest.readInt() // param7 = resolveQuantifier()
            else -> null
        }
        if (kind == WiredKind.SELECTOR) {
            // param7 = resolveFilterField(), param8 = resolveInverseField()
            filter = habboRequest.readBoolean()
            inverse = habboRequest.readBoolean()
        } else {
            filter = null
            inverse = null
        }

        // param7/param8: furniSourceTypes (códigos AS3: 0 = Triggering Furni, 100 = Selected, 200 = Selector)
        val furniSourcesCount = habboRequest.readInt()
        furniSources = buildList {
            repeat(furniSourcesCount) { _ ->
                WiredFurniSource.fromCode(habboRequest.readInt())?.let { add(it) }
            }
        }

        // param8/param9: userSourceTypes (Ex: 0 = Triggering User)
        val userSourcesCount = habboRequest.readInt()
        userSources = buildList {
            repeat(userSourcesCount) { _ ->
                WiredUserSource.fromCode(habboRequest.readInt())?.let { add(it) }
            }
        }

        // param3: resolveVariableIds() — nasce junto com o WiredContext (2024-05-06):
        // Long até < 2024-12-12, String/UTF depois
        variableIds = if (wire.hasVariableIds) {
            val variableIdsCount = habboRequest.readInt()
            buildList {
                repeat(variableIdsCount) { _ ->
                    add(
                        if (wire.variableIdsAsLong) habboRequest.readLong().toString()
                        else habboRequest.readUTF()
                    )
                }
            }
        } else {
            null
        }

        // param6: getStuffIds2() (mobis secundários)
        stuffIds2 = if (wire.hasStuffIds2) {
            val stuffIds2Count = habboRequest.readInt()
            buildList {
                repeat(stuffIds2Count) { _ ->
                    add(habboRequest.readInt())
                }
            }
        } else {
            null
        }
    } else {
        // Delay apenas para effects na versão antiga
        delay = if (kind == WiredKind.EFFECT) habboRequest.readInt() else null
        quantifier = null
        filter = null
        inverse = null
        userSources = null
        variableIds = null
        stuffIds2 = null

        // stuffTypeSelectionCode (Flash clássico): 1 = triggering furni, senão selected items
        val stuffTypeSelectionCode = habboRequest.readInt()
        furniSources = if (stuffTypeSelectionCode == 1) {
            listOf(WiredFurniSource.TRIGGERING_ITEM)
        } else {
            listOf(WiredFurniSource.SELECTED_ITEMS)
        }
    }

    return WiredSaveWire(
        options = options,
        message = message,
        stuffIds = stuffIds,
        delay = delay,
        quantifier = quantifier,
        filter = filter,
        inverse = inverse,
        furniSources = furniSources,
        userSources = userSources,
        variableIds = variableIds,
        stuffIds2 = stuffIds2,
    )
}

/**
 * Entrada do serializer do diálogo (outgoing): tudo o que o wire precisa, já resolvido
 * pelo applier (WiredItem.writeDialog) a partir de WiredData + RoomItem + política da
 * subclasse. O writer é puro: só decide ordem e forma por era.
 */
data class WiredDialogWireData(
    val kind: WiredKind?,
    val spriteId: Int,
    val roomItemId: Int,
    val requiresItems: Boolean,
    val stuffTypeSelectionEnabled: Boolean,
    val stuffIds: List<Int>,
    val stuffIds2: List<Int>,
    val message: String,
    val options: List<Int>,
    val code: Int,
    val delay: Int,
    val filter: Boolean,
    val inverse: Boolean,
    /** stuffTypeSelectionCode do Flash clássico (política da subclasse). */
    val selectionCode: Int,
    val variableIds: List<String>,
    /** Códigos de fonte prontos (do DB, ou o default da subclasse se vazio). */
    val furniSourceCodes: List<Int>,
    val userSourceCodes: List<Int>,
    /** Configuração dos menus dropdown de origem (política da subclasse, _SafeStr_7491..7494). */
    val furniSourceGroups: List<List<WiredFurniSource>>,
    val userSourceGroups: List<List<WiredUserSource>>,
    val defaultFurniSourceGroups: List<WiredFurniSource>,
    val defaultUserSourceGroups: List<WiredUserSource>,
    /** Blocos de WiredContext (domain) — o writer só os serializa, era-gated. */
    val wiredContextBlocks: List<WiredContextBlock>,
)

fun writeWiredDialogWire(habboResponse: HabboResponse, version: HabboVersion, data: WiredDialogWireData) {
    val wire = WiredDialogWire(version)
    val habboAir = wire.isAirDialog

    with(habboResponse) {
        if (!habboAir && version.isVersionAtLeast(2011, 2, 22) && version.isVersionBefore(2023, 4, 14)) {
            writeBoolean(data.stuffTypeSelectionEnabled)
        }

        if (data.requiresItems) {
            writeInt(20) // selectable items
            writeInt(data.stuffIds.size)
            data.stuffIds.forEach { writeInt(it) }

            if (wire.hasStuffIds2) {
                writeInt(data.stuffIds2.size)
                data.stuffIds2.forEach { writeInt(it) }
            }
        } else {
            writeInt(0) // selectable items
            writeInt(0) // items

            if (wire.hasStuffIds2) {
                writeInt(0) // stuffIds2
            }
        }

        writeInt(data.spriteId)
        writeInt(data.roomItemId)

        // settings: text + count + options (+ selectionCode no Flash clássico)
        val settingsCount = when {
            data.message.isNotEmpty() && data.options.isNotEmpty() -> data.options.size
            data.message.isNotEmpty() -> 0
            data.options.isNotEmpty() -> data.options.size
            else -> 0
        }
        writeUTF(data.message)
        writeInt(settingsCount)
        data.options.forEach { writeInt(it) }
        if (!habboAir) {
            writeInt(data.selectionCode)
        }

        if (habboAir) {
            if (wire.hasVariableIds) {
                writeInt(data.variableIds.size)
                data.variableIds.forEach {
                    if (wire.variableIdsAsLong) {
                        writeLong(it.toLongOrNull() ?: 0L)
                    } else {
                        writeUTF(it)
                    }
                }
            }

            writeInt(data.furniSourceCodes.size)
            data.furniSourceCodes.forEach { writeInt(it) }

            writeInt(data.userSourceCodes.size)
            data.userSourceCodes.forEach { writeInt(it) }
        }

        // Para o Habbo antes do AIR, a classe pai termina antes de CODE
        writeInt(data.code)

        when {
            data.kind == WiredKind.EFFECT -> writeInt(data.delay)
            habboAir && data.kind == WiredKind.SELECTOR -> {
                writeBoolean(data.filter)
                writeBoolean(data.inverse)
            }

            habboAir && data.kind == WiredKind.CONDITION -> {
                writeInt(0) // quantifierCode (TODO)
                if (wire.hasConditionInvert) {
                    writeBoolean(false) // isInvert
                }
            }
        }

        if (habboAir) {
            // A aba "Avançado" só deve aparecer se o Wired tiver opções de fonte configuradas
            val hasAdvancedMode = data.furniSourceGroups.sumOf { it.size } > 1 ||
                    data.userSourceGroups.sumOf { it.size } > 1 ||
                    data.furniSourceGroups.size > 1 ||
                    data.userSourceGroups.size > 1
            writeBoolean(hasAdvancedMode)

            // _SafeStr_7491: getAllowedFurniSources
            writeInt(data.furniSourceGroups.size)
            data.furniSourceGroups.forEach { group ->
                writeInt(group.size)
                group.forEach { writeInt(it.code) }
            }

            // _SafeStr_7492: getAllowedUserSources
            writeInt(data.userSourceGroups.size)
            data.userSourceGroups.forEach { group ->
                writeInt(group.size)
                group.forEach { writeInt(it.code) }
            }

            // _SafeStr_7493: defaultFurniSources
            writeInt(data.defaultFurniSourceGroups.size)
            data.defaultFurniSourceGroups.forEach { writeInt(it.code) }

            // _SafeStr_7494: defaultUserSources
            writeInt(data.defaultUserSourceGroups.size)
            data.defaultUserSourceGroups.forEach { writeInt(it.code) }

            writeBoolean(false) // allowWallFurni

            if (data.kind == WiredKind.CONDITION) {
                if (wire.hasWiredContext) {
                    writeByte(0) // quantifierType
                    writeBoolean(false) // isInvert
                } else {
                    writeByte(0) // quantifierType lido no final do ctor da subclasse em 2023
                }
            }

            if (wire.hasWiredContext) {
                writeInt(data.wiredContextBlocks.size)
                data.wiredContextBlocks.forEach { serialize(it) }
            }

            if (wire.hasStuffIds2) {
                writeInt(0) // defaultIntParams: size
            }
        } else {
            // Legacy format - conflictingActions
            if (data.kind == WiredKind.TRIGGER) {
                writeInt(0) // blockedTriggers: TODO
            } else if (data.kind == WiredKind.EFFECT) {
                writeInt(0) // blockedActions: TODO
            }
        }
    }
}
