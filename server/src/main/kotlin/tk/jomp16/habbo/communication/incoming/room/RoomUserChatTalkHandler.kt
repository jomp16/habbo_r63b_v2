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

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.game.room.tasks.ChatType
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUserChatTalkHandler {
    @Handler(Incoming.ROOM_USER_CHAT, Incoming.ROOM_USER_SHOUT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null) return

        val (message, bubble) = parse(habboRequest) ?: return

        // todo: check if user can use bubble
        // todo: check if user is muted
        // todo: word filter

        habboSession.roomUser?.chat(habboSession.roomUser!!.virtualID, message, bubble, if (habboRequest.headerId == Incoming.ROOM_USER_SHOUT) ChatType.SHOUT else ChatType.CHAT, false)
    }

    private fun parse(habboRequest: HabboRequest): Pair<String, Int>? {
        var message = habboRequest.readUTF()

        if (message.isEmpty()) return null
        if (message.length > 100) message = message.substring(0, 100)

        val bubble = habboRequest.readInt()

        return Pair(message, bubble)
    }
}