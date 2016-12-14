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

package tk.jomp16.habbo.communication.outgoing.room

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomInfoResponse {
    @Response(Outgoing.ROOM_INFO)
    fun response(habboResponse: HabboResponse, habboSession: HabboSession, room: Room, isLoading: Boolean, checkEntry: Boolean) {
        habboResponse.apply {
            writeBoolean(isLoading)

            serialize(room, true, isLoading)

            writeBoolean(checkEntry)
            writeBoolean(false)
            writeBoolean(false) // bypass bell, etc
            writeBoolean(false) // todo: room muted
            writeInt(room.roomData.muteSettings)
            writeInt(room.roomData.kickSettings)
            writeInt(room.roomData.banSettings)
            writeBoolean(room.hasRights(habboSession, true))
            writeInt(room.roomData.chatType)
            writeInt(room.roomData.chatBalloon)
            writeInt(room.roomData.chatSpeed)
            writeInt(room.roomData.chatMaxDistance)
            writeInt(room.roomData.chatFloodProtection)
        }
    }
}