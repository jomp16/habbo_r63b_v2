/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.communication.incoming.room

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.QueuedHabboResponse
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.room.RoomState
import tk.jomp16.habbo.game.user.HabboSession
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER")
class RoomSaveSettingsHandler {
    @Handler(Incoming.ROOM_SAVE_SETTINGS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        val roomId = habboRequest.readInt()
        val room = HabboServer.habboGame.roomManager.rooms[roomId] ?: return

        if (!room.hasRights(habboSession, true)) return

        var roomName = habboRequest.readUTF()

        if (roomName.isEmpty()) return

        var roomDescription = habboRequest.readUTF()
        var roomState = RoomState.fromIntValue(habboRequest.readInt())
        var roomPassword = habboRequest.readUTF()

        if (roomState === RoomState.PASSWORD && roomPassword.isEmpty()) roomState = RoomState.OPEN

        var roomMaxUsers = habboRequest.readInt()

        if (roomMaxUsers < 10) roomMaxUsers = 10

        val roomCategoryId = habboRequest.readInt()

        val tags: MutableList<String> = ArrayList()

        val roomTagCount = habboRequest.readInt()

        (0..roomTagCount - 1).forEach { i ->
            val tag = habboRequest.readUTF().replace(",", "")

            if (!tag.isEmpty() && tag.length <= 30) tags += tag
        }

        val roomTradeState = habboRequest.readInt()
        val roomAllowPets = habboRequest.readBoolean()
        val roomAllowPetsEat = habboRequest.readBoolean()
        val roomAllowWalkThrough = habboRequest.readBoolean()
        var roomHideWalls = habboRequest.readBoolean()
        var roomWallThickness = habboRequest.readInt()
        var roomFloorThickness = habboRequest.readInt()
        val whoCanMute = habboRequest.readInt()
        val whoCanKick = habboRequest.readInt()
        val whoCanBan = habboRequest.readInt()
        val chatType = habboRequest.readInt()
        val chatBalloon = habboRequest.readInt()
        val chatSpeed = habboRequest.readInt()
        var chatMaxDistance = habboRequest.readInt()
        var chatFloodProtection = habboRequest.readInt()

        if (roomHideWalls && !habboSession.habboSubscription.validUserSubscription) roomHideWalls = false
        if (roomWallThickness < -2 || roomWallThickness > 1) roomWallThickness = 0
        if (roomFloorThickness < -2 || roomFloorThickness > 1) roomFloorThickness = 0
        if (roomName.length > 60) roomName = roomName.substring(0, 60)
        if (roomDescription.length > 128) roomDescription = roomDescription.substring(0, 128)
        if (roomPassword.length > 64) roomPassword = roomPassword.substring(0, 64)
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

        if (habboSession.currentRoom == null) {
            val queuedHabboResponse = QueuedHabboResponse()

            queuedHabboResponse += Outgoing.ROOM_SETTINGS_SAVED to arrayOf(roomId)
            queuedHabboResponse += Outgoing.ROOM_INFO_UPDATED to arrayOf(roomId)
            queuedHabboResponse += Outgoing.ROOM_VISUALIZATION_THICKNESS to arrayOf(room.roomData.hideWall, room.roomData.wallThick, room.roomData.floorThick)

            habboSession.sendQueuedHabboResponse(queuedHabboResponse)
        }

        room.sendHabboResponse(Outgoing.ROOM_SETTINGS_SAVED, roomId)
        room.sendHabboResponse(Outgoing.ROOM_INFO_UPDATED, roomId)
        room.sendHabboResponse(Outgoing.ROOM_VISUALIZATION_THICKNESS, room.roomData.hideWall, room.roomData.wallThick, room.roomData.floorThick)
    }
}