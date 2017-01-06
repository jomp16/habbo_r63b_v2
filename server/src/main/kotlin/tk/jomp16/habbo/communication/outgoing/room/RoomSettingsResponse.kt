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

package tk.jomp16.habbo.communication.outgoing.room

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.room.Room

@Suppress("unused", "UNUSED_PARAMETER")
class RoomSettingsResponse {
    @Response(Outgoing.ROOM_SETTINGS)
    fun handle(habboResponse: HabboResponse, room: Room) {
        habboResponse.apply {
            writeInt(room.roomData.id)
            writeUTF(room.roomData.caption)
            writeUTF(room.roomData.description)
            writeInt(room.roomData.state.state)
            writeInt(room.roomData.category)
            writeInt(room.roomData.usersMax)
            writeInt(if (room.roomModel.mapSizeX * room.roomModel.mapSizeY > 100) 50 else 25)

            writeInt(room.roomData.tags.size)

            room.roomData.tags.forEach { writeUTF(it) }

            writeInt(room.roomData.tradeState)
            writeInt(if (room.roomData.allowPets) 1 else 0)
            writeInt(if (room.roomData.allowPetsEat) 1 else 0)
            writeInt(if (room.roomData.allowWalkThrough) 1 else 0)
            writeInt(if (room.roomData.hideWall) 1 else 0)
            writeInt(room.roomData.floorThick)
            writeInt(room.roomData.wallThick)
            writeInt(room.roomData.chatType)
            writeInt(room.roomData.chatBalloon)
            writeInt(room.roomData.chatSpeed)
            writeInt(room.roomData.chatMaxDistance)
            writeInt(room.roomData.chatFloodProtection)
            writeBoolean(true)
            writeInt(room.roomData.muteSettings)
            writeInt(room.roomData.kickSettings)
            writeInt(room.roomData.banSettings)
        }
    }
}