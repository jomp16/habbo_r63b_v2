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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.room.RoomVisualizationThicknessData
import ovh.rwx.habbo.game.room.RoomState
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomSaveSettingsHandler {
    @Handler(Incoming.ROOM_SAVE_SETTINGS)
    @HandlerR63A(IncomingR63A.ROOM_SAVE_SETTINGS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val roomId = habboRequest.readInt()
        val room = HabboServer.habboGame.roomManager.rooms[roomId] ?: return

        if (!room.userManager.hasRights(habboSession, true)) return
        var roomName = habboRequest.readUTF().trim()

        if (roomName.isBlank()) return
        var roomDescription = habboRequest.readUTF().trim()
        var roomState = RoomState.fromIntValue(habboRequest.readInt())
        val roomPassword = habboRequest.readUTF().trim()

        if (roomState == RoomState.PASSWORD && roomPassword.isBlank()) roomState = RoomState.OPEN
        var roomMaxUsers = habboRequest.readInt()

        if (roomMaxUsers < 10) roomMaxUsers = 10

        // Configurações legadas do Habbo Beta (2009 até 2010-01-22)
        var legacyAllowTrading: Boolean? = null
        var legacyShowOwnerName: Boolean? = null
        if (habboSession.isVersionBefore(2010, 1, 22)) {
            val allowDoorbell = habboRequest.readInt() == 1 // field 7: allowDoorbell / allowDirectAccess
            legacyAllowTrading = habboRequest.readInt() == 1 // field 8: allowTrading
            legacyShowOwnerName = habboRequest.readInt() == 1 // field 9: showOwnerName
        }

        val roomCategoryId = habboRequest.readInt()
        val tags: MutableList<String> = mutableListOf()
        val roomTagCount = habboRequest.readInt()

        repeat(roomTagCount) {
            val tag = habboRequest.readUTF().replace(",", "").trim()

            if (tag.isNotBlank() && tag.length <= 30) tags += tag
        }

        val isLegacyHabbo = habboSession.isVersionBefore(2011, 9, 30)

        // TradeState:
        // - RELEASE63 (2011-11-03+): Int com 3 estados (0=desativado, 1=direitos, 2=todos)
        // - 2009 (pré-2010-01-22): Booleano allowTrading (true = 2 [todos], false = 0 [desativado])
        val roomTradeState = when {
            habboSession.isVersionAtLeast(2011, 11, 3) -> habboRequest.readInt()
            legacyAllowTrading != null -> if (legacyAllowTrading) 2 else 0
            else -> room.roomData.tradeState
        }

        // Pets, comida e atravessar:
        var roomAllowPets = room.roomData.allowPets
        var roomAllowPetsEat = room.roomData.allowPetsEat
        var roomAllowWalkThrough = room.roomData.allowWalkThrough

        when {
            // RELEASE45+ (2010-02-05): 3 booleanos (allowPets, allowPetsEat, allowWalkThrough)
            habboSession.isVersionAtLeast(2010, 2, 5) -> {
                roomAllowPets = habboRequest.readBoolean()
                roomAllowPetsEat = habboRequest.readBoolean()
                roomAllowWalkThrough = habboRequest.readBoolean()
            }
            // RELEASE41+ (2009-11-27): 2 booleanos (allowPets, allowPetsEat)
            habboSession.isVersionAtLeast(2009, 11, 27) -> {
                roomAllowPets = habboRequest.readBoolean()
                roomAllowPetsEat = habboRequest.readBoolean()
            }
            // RELEASE40 (2009-11-13 até 2009-11-27): apenas 1 booleano (allowPets)
            habboSession.isVersionAtLeast(2009, 11, 13) -> {
                roomAllowPets = habboRequest.readBoolean()
            }
        }

        // Esconder paredes (VIP): introduzido na RELEASE52 em 2010-05-27
        var roomHideWalls = if (habboSession.isVersionAtLeast(2010, 5, 27)) {
            habboRequest.readBoolean()
        } else {
            room.roomData.hideWall
        }

        // Espessura de parede e chão: introduzidos juntos na RELEASE63-33647 em 2011-05-13
        var roomWallThickness = room.roomData.wallThick
        var roomFloorThickness = room.roomData.floorThick
        if (habboSession.isVersionAtLeast(2011, 5, 13)) {
            roomWallThickness = habboRequest.readInt()
            roomFloorThickness = habboRequest.readInt()
        }

        // Moderação de quarto (mute, kick, ban): introduzida em 2012-11-19 (RELEASE63-201211192303-273844286)
        val whoCanMute: Int
        val whoCanKick: Int
        val whoCanBan: Int
        if (habboSession.isVersionAtLeast(2012, 11, 19)) {
            whoCanMute = habboRequest.readInt()
            whoCanKick = habboRequest.readInt()
            whoCanBan = habboRequest.readInt()
        } else {
            whoCanMute = room.roomData.muteSettings
            whoCanKick = room.roomData.kickSettings
            whoCanBan = room.roomData.banSettings
        }

        // Configurações de chat
        var chatType = room.roomData.chatType
        var chatBalloon = room.roomData.chatBalloon
        var chatSpeed = room.roomData.chatSpeed
        var chatMaxDistance = room.roomData.chatMaxDistance
        var chatFloodProtection = room.roomData.chatFloodProtection

        when {
            // AIR Moderno (WIN63 2026-05-18+): novo formato com chatFloodSensitivity e flags afk/sleep
            habboSession.isAir && habboSession.isVersionAtLeast(2026, 5, 18) -> {
                chatFloodProtection = habboRequest.readInt()
                val leaveOnDoorTileEnabled = habboRequest.readBoolean()
                val idleSleepEnabled = habboRequest.readBoolean()
                val idleSleepTimeoutSeconds = habboRequest.readInt()
                val idleAutokickEnabled = habboRequest.readBoolean()
                val idleAutokickTimeoutSeconds = habboRequest.readInt()
                val muteAllPets = habboRequest.readBoolean()

                room.roomData.leaveOnDoorTileEnabled = leaveOnDoorTileEnabled
                room.roomData.idleSleepEnabled = idleSleepEnabled
                room.roomData.idleSleepTimeoutSeconds = idleSleepTimeoutSeconds
                room.roomData.idleAutokickEnabled = idleAutokickEnabled
                room.roomData.idleAutokickTimeoutSeconds = idleAutokickTimeoutSeconds
                room.roomData.muteAllPets = muteAllPets
            }

            // Flash Moderno e AIR pré-2026 (PRODUCTION-20151201+ até 2026-05-18): 5 Ints + 1 Boolean
            habboSession.isVersionAtLeast(2015, 12, 1) -> {
                chatType = habboRequest.readInt()
                chatBalloon = habboRequest.readInt()
                chatSpeed = habboRequest.readInt()
                chatMaxDistance = habboRequest.readInt()
                chatFloodProtection = habboRequest.readInt()
                room.roomData.allowNavigatorDynamicCats = habboRequest.readBoolean()
            }

            // R63B Intermediário (2013-06-10 até 2014-10-15): 3 Ints
            habboSession.isVersionAtLeast(2013, 6, 10) -> {
                chatType = habboRequest.readInt()
                chatBalloon = habboRequest.readInt()
                chatSpeed = habboRequest.readInt()
            }
        }

        if (roomHideWalls && !habboSession.habboSubscription.validUserSubscription) roomHideWalls = false
        if (roomWallThickness < -2 || roomWallThickness > 1) roomWallThickness = 0
        if (roomFloorThickness < -2 || roomFloorThickness > 1) roomFloorThickness = 0
        if (roomName.length > 60) roomName = roomName.substring(0, 60).trim()
        if (roomDescription.length > 128) roomDescription = roomDescription.substring(0, 128).trim()
        if (chatMaxDistance > 99) chatMaxDistance = 99
        if (chatFloodProtection > 2) chatFloodProtection = 2

        room.roomData.name = roomName
        room.roomData.description = roomDescription
        room.roomData.state = roomState
        room.roomData.password = HabboServer.habboGame.passwordEncryptor.encryptPassword(roomPassword)
        room.roomData.usersMax = roomMaxUsers
        room.roomData.category = roomCategoryId
        room.roomData.tags = tags
        room.roomData.tradeState = roomTradeState
        room.roomData.allowPets = roomAllowPets
        room.roomData.allowPetsEat = roomAllowPetsEat
        room.roomData.allowWalkThrough = roomAllowWalkThrough
        room.roomData.hideWall = roomHideWalls
        room.roomData.wallThick = roomWallThickness
        room.roomData.floorThick = roomFloorThickness
        room.roomData.muteSettings = whoCanMute
        room.roomData.kickSettings = whoCanKick
        room.roomData.banSettings = whoCanBan
        room.roomData.chatType = chatType
        room.roomData.chatBalloon = chatBalloon
        room.roomData.chatSpeed = chatSpeed
        room.roomData.chatMaxDistance = chatMaxDistance
        room.roomData.chatFloodProtection = chatFloodProtection
        room.roomData.markDirty()

        val thicknessData = RoomVisualizationThicknessData(
            room.roomData.hideWall,
            room.roomData.wallThick,
            room.roomData.floorThick
        )

        if (habboSession.currentRoom == null) {
            habboSession.sendResponse(Outgoing.ROOM_SETTINGS_SAVED, OutgoingR63A.ROOM_SETTINGS_SAVED, roomId)
            if (!isLegacyHabbo) {
                habboSession.sendHabboResponse(Outgoing.ROOM_INFO_UPDATED, roomId)
            }
            habboSession.sendResponse(
                Outgoing.ROOM_VISUALIZATION_THICKNESS,
                OutgoingR63A.ROOM_VISUALIZATION_THICKNESS,
                thicknessData
            )
        }

        room.sendResponse(Outgoing.ROOM_SETTINGS_SAVED, OutgoingR63A.ROOM_SETTINGS_SAVED, roomId)
        if (!isLegacyHabbo) {
            room.sendHabboResponse(Outgoing.ROOM_INFO_UPDATED, roomId)
        }
        room.sendResponse(
            Outgoing.ROOM_VISUALIZATION_THICKNESS,
            OutgoingR63A.ROOM_VISUALIZATION_THICKNESS,
            thicknessData
        )
    }
}