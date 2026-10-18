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
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.room.RoomVisualizationThicknessData
import ovh.rwx.habbo.game.room.RoomState
import ovh.rwx.habbo.game.user.HabboSession

/**
 * Applier fino do ROOM_SAVE_SETTINGS: o parse temporal puro vive em [parseRoomSettingsWire]
 * (testável por era com bytes sintéticos); aqui fica só a política de domínio — clamps,
 * preservação de campos ausentes e resposta.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class RoomSaveSettingsHandler {
    @Handler(Incoming.ROOM_SAVE_SETTINGS)
    @HandlerR63A(IncomingR63A.ROOM_SAVE_SETTINGS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val roomId = habboRequest.readInt()
        val room = HabboServer.habboGame.roomManager.rooms[roomId] ?: return

        if (!room.userManager.hasRights(habboSession, true)) return

        val wire = parseRoomSettingsWire(habboRequest, habboSession.habboVersion)

        if (wire.name.isBlank()) return

        var roomState = RoomState.fromIntValue(wire.stateValue)
        if (roomState == RoomState.PASSWORD && wire.password.isBlank()) roomState = RoomState.OPEN
        var roomMaxUsers = wire.maxUsers
        if (roomMaxUsers < 10) roomMaxUsers = 10
        var roomHideWalls = wire.hideWalls ?: room.roomData.hideWall
        if (roomHideWalls && !habboSession.habboSubscription.validUserSubscription) roomHideWalls = false
        var roomWallThickness = wire.wallThickness ?: room.roomData.wallThick
        if (roomWallThickness < -2 || roomWallThickness > 1) roomWallThickness = 0
        var roomFloorThickness = wire.floorThickness ?: room.roomData.floorThick
        if (roomFloorThickness < -2 || roomFloorThickness > 1) roomFloorThickness = 0

        var roomName = wire.name
        if (roomName.length > 60) roomName = roomName.substring(0, 60).trim()
        var roomDescription = wire.description
        if (roomDescription.length > 128) roomDescription = roomDescription.substring(0, 128).trim()
        var chatMaxDistance = wire.chatMaxDistance ?: room.roomData.chatMaxDistance
        if (chatMaxDistance > 99) chatMaxDistance = 99
        var chatFloodProtection = wire.chatFloodProtection ?: room.roomData.chatFloodProtection
        if (chatFloodProtection > 2) chatFloodProtection = 2

        room.roomData.name = roomName
        room.roomData.description = roomDescription
        room.roomData.state = roomState
        room.roomData.password = HabboServer.habboGame.passwordEncryptor.encryptPassword(wire.password)
        room.roomData.usersMax = roomMaxUsers
        room.roomData.category = wire.categoryId
        room.roomData.tags = wire.tags
        wire.tradeState?.let { room.roomData.tradeState = it }
        wire.allowPets?.let { room.roomData.allowPets = it }
        wire.allowPetsEat?.let { room.roomData.allowPetsEat = it }
        wire.allowWalkThrough?.let { room.roomData.allowWalkThrough = it }
        room.roomData.hideWall = roomHideWalls
        room.roomData.wallThick = roomWallThickness
        room.roomData.floorThick = roomFloorThickness
        wire.whoCanMute?.let { room.roomData.muteSettings = it }
        wire.whoCanKick?.let { room.roomData.kickSettings = it }
        wire.whoCanBan?.let { room.roomData.banSettings = it }
        wire.chatType?.let { room.roomData.chatType = it }
        wire.chatBalloon?.let { room.roomData.chatBalloon = it }
        wire.chatSpeed?.let { room.roomData.chatSpeed = it }
        room.roomData.chatMaxDistance = chatMaxDistance
        room.roomData.chatFloodProtection = chatFloodProtection
        wire.allowNavigatorDynamicCats?.let { room.roomData.allowNavigatorDynamicCats = it }
        wire.leaveOnDoorTileEnabled?.let { room.roomData.leaveOnDoorTileEnabled = it }
        wire.idleSleepEnabled?.let { room.roomData.idleSleepEnabled = it }
        wire.idleSleepTimeoutSeconds?.let { room.roomData.idleSleepTimeoutSeconds = it }
        wire.idleAutokickEnabled?.let { room.roomData.idleAutokickEnabled = it }
        wire.idleAutokickTimeoutSeconds?.let { room.roomData.idleAutokickTimeoutSeconds = it }
        wire.muteAllPets?.let { room.roomData.muteAllPets = it }
        room.roomData.markDirty()

        val isLegacyHabbo = habboSession.isVersionBefore(2011, 9, 30)
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
