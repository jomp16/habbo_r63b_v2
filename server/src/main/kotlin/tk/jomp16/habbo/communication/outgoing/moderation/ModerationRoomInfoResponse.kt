/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.communication.outgoing.moderation

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.room.Room

@Suppress("unused", "UNUSED_PARAMETER")
class ModerationRoomInfoResponse {
    @Response(Outgoing.MODERATION_ROOM_INFO)
    fun handle(habboResponse: HabboResponse, room: Room) {
        habboResponse.apply {
            writeInt(room.roomData.id)
            writeInt(room.roomUsers.size)
            writeBoolean(room.roomUsers.values.filter { it.habboSession != null }.any { room.roomData.ownerId == it.habboSession!!.userInformation.id })
            writeInt(room.roomData.ownerId)
            writeUTF(room.roomData.ownerName)
            writeBoolean(true) // room data, always true because I have the info
            writeUTF(room.roomData.caption)
            writeUTF(room.roomData.description)
            writeInt(room.roomData.tags.size)

            room.roomData.tags.forEach { writeUTF(it) }
        }
    }
}