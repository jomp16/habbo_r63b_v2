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

package ovh.rwx.habbo.communication.incoming.room

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HabboVersion

/**
 * Wire puro do ROOM_SAVE_SETTINGS (parser temporal por era, 2009-11-13 → AIR 2026-05-18).
 * A arqueologia de wire vive aqui — o handler vira um applier fino de política de domínio.
 *
 * Campos null = a compilação cliente não envia; o applier preserva o estado existente
 * (GLOSSARY: "Preservação de Estado em Campos Opcionais Ausentes").
 *
 * Evolução temporal (bissecções AS3 anteriores):
 * - < 2010-01-22: 3 ints legados do Beta (allowDoorbell drenado, allowTrading, showOwnerName drenado);
 *   tradeState é o booleano legado mapeado (true=2 todos, false=0 desativado).
 * - ≥ 2009-11-13: allowPets; ≥ 2009-11-27: +allowPetsEat; ≥ 2010-02-05: +allowWalkThrough.
 * - ≥ 2010-05-27: hideWalls; ≥ 2011-05-13: wall/floorThickness; ≥ 2011-11-03: tradeState como Int.
 * - ≥ 2012-11-19: whoCanMute/Kick/Ban; chat: ≥ 2013-06-10 (3 ints), ≥ 2015-12-01 (5 ints + dynCats),
 *   AIR ≥ 2026-05-18 (chatFloodSensitivity + 6 flags AFK/Sleep/Porta/Pets — os outros campos de chat
 *   foram extintos, GLOSSARY: "AIR Moderno").
 */
data class RoomSettingsWire(
    val name: String,
    val description: String,
    val stateValue: Int,
    val password: String,
    val maxUsers: Int,
    val categoryId: Int,
    val tags: List<String>,
    val tradeState: Int?,
    val allowPets: Boolean?,
    val allowPetsEat: Boolean?,
    val allowWalkThrough: Boolean?,
    val hideWalls: Boolean?,
    val wallThickness: Int?,
    val floorThickness: Int?,
    val whoCanMute: Int?,
    val whoCanKick: Int?,
    val whoCanBan: Int?,
    val chatType: Int?,
    val chatBalloon: Int?,
    val chatSpeed: Int?,
    val chatMaxDistance: Int?,
    val chatFloodProtection: Int?,
    val allowNavigatorDynamicCats: Boolean?,
    val leaveOnDoorTileEnabled: Boolean?,
    val idleSleepEnabled: Boolean?,
    val idleSleepTimeoutSeconds: Int?,
    val idleAutokickEnabled: Boolean?,
    val idleAutokickTimeoutSeconds: Int?,
    val muteAllPets: Boolean?,
)

fun parseRoomSettingsWire(habboRequest: HabboRequest, version: HabboVersion): RoomSettingsWire {
    val name = habboRequest.readUTF().trim()
    val description = habboRequest.readUTF().trim()
    val stateValue = habboRequest.readInt()
    val password = habboRequest.readUTF().trim()
    val maxUsers = habboRequest.readInt()

    // Beta (2009 até 2010-01-22): 3 ints legados — allowDoorbell e showOwnerName são drenados
    var legacyAllowTrading: Boolean? = null
    if (version.isVersionBefore(2010, 1, 22)) {
        habboRequest.readInt() // allowDoorbell / allowDirectAccess — efêmero, não persiste
        legacyAllowTrading = habboRequest.readInt() == 1
        habboRequest.readInt() // showOwnerName — efêmero, não persiste
    }

    val categoryId = habboRequest.readInt()
    val tags = mutableListOf<String>()
    val tagCount = habboRequest.readInt()

    repeat(tagCount) {
        val tag = habboRequest.readUTF().replace(",", "").trim()
        if (tag.isNotBlank() && tag.length <= 30) tags += tag
    }

    val tradeState = when {
        version.isVersionAtLeast(2011, 11, 3) -> habboRequest.readInt()
        legacyAllowTrading != null -> if (legacyAllowTrading) 2 else 0
        else -> null
    }

    var allowPets: Boolean? = null
    var allowPetsEat: Boolean? = null
    var allowWalkThrough: Boolean? = null
    when {
        version.isVersionAtLeast(2010, 2, 5) -> {
            allowPets = habboRequest.readBoolean()
            allowPetsEat = habboRequest.readBoolean()
            allowWalkThrough = habboRequest.readBoolean()
        }
        version.isVersionAtLeast(2009, 11, 27) -> {
            allowPets = habboRequest.readBoolean()
            allowPetsEat = habboRequest.readBoolean()
        }
        version.isVersionAtLeast(2009, 11, 13) -> allowPets = habboRequest.readBoolean()
    }

    val hideWalls = if (version.isVersionAtLeast(2010, 5, 27)) habboRequest.readBoolean() else null
    val wallThickness: Int?
    val floorThickness: Int?
    if (version.isVersionAtLeast(2011, 5, 13)) {
        wallThickness = habboRequest.readInt()
        floorThickness = habboRequest.readInt()
    } else {
        wallThickness = null
        floorThickness = null
    }

    val whoCanMute: Int?
    val whoCanKick: Int?
    val whoCanBan: Int?
    if (version.isVersionAtLeast(2012, 11, 19)) {
        whoCanMute = habboRequest.readInt()
        whoCanKick = habboRequest.readInt()
        whoCanBan = habboRequest.readInt()
    } else {
        whoCanMute = null
        whoCanKick = null
        whoCanBan = null
    }

    var chatType: Int? = null
    var chatBalloon: Int? = null
    var chatSpeed: Int? = null
    var chatMaxDistance: Int? = null
    var chatFloodProtection: Int? = null
    var allowNavigatorDynamicCats: Boolean? = null
    var leaveOnDoorTileEnabled: Boolean? = null
    var idleSleepEnabled: Boolean? = null
    var idleSleepTimeoutSeconds: Int? = null
    var idleAutokickEnabled: Boolean? = null
    var idleAutokickTimeoutSeconds: Int? = null
    var muteAllPets: Boolean? = null

    when {
        version.isAir && version.isVersionAtLeast(2026, 5, 18) -> {
            chatFloodProtection = habboRequest.readInt()
            leaveOnDoorTileEnabled = habboRequest.readBoolean()
            idleSleepEnabled = habboRequest.readBoolean()
            idleSleepTimeoutSeconds = habboRequest.readInt()
            idleAutokickEnabled = habboRequest.readBoolean()
            idleAutokickTimeoutSeconds = habboRequest.readInt()
            muteAllPets = habboRequest.readBoolean()
        }
        version.isVersionAtLeast(2015, 12, 1) -> {
            chatType = habboRequest.readInt()
            chatBalloon = habboRequest.readInt()
            chatSpeed = habboRequest.readInt()
            chatMaxDistance = habboRequest.readInt()
            chatFloodProtection = habboRequest.readInt()
            allowNavigatorDynamicCats = habboRequest.readBoolean()
        }
        version.isVersionAtLeast(2013, 6, 10) -> {
            chatType = habboRequest.readInt()
            chatBalloon = habboRequest.readInt()
            chatSpeed = habboRequest.readInt()
        }
    }

    return RoomSettingsWire(
        name = name,
        description = description,
        stateValue = stateValue,
        password = password,
        maxUsers = maxUsers,
        categoryId = categoryId,
        tags = tags,
        tradeState = tradeState,
        allowPets = allowPets,
        allowPetsEat = allowPetsEat,
        allowWalkThrough = allowWalkThrough,
        hideWalls = hideWalls,
        wallThickness = wallThickness,
        floorThickness = floorThickness,
        whoCanMute = whoCanMute,
        whoCanKick = whoCanKick,
        whoCanBan = whoCanBan,
        chatType = chatType,
        chatBalloon = chatBalloon,
        chatSpeed = chatSpeed,
        chatMaxDistance = chatMaxDistance,
        chatFloodProtection = chatFloodProtection,
        allowNavigatorDynamicCats = allowNavigatorDynamicCats,
        leaveOnDoorTileEnabled = leaveOnDoorTileEnabled,
        idleSleepEnabled = idleSleepEnabled,
        idleSleepTimeoutSeconds = idleSleepTimeoutSeconds,
        idleAutokickEnabled = idleAutokickEnabled,
        idleAutokickTimeoutSeconds = idleAutokickTimeoutSeconds,
        muteAllPets = muteAllPets,
    )
}
