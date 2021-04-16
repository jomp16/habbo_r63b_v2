/*
 * Copyright (C) 2015-2021 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.game.room.tasks.ChatType
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUserChatTalkHandler {
    @Handler(Incoming.ROOM_USER_CHAT, Incoming.ROOM_USER_SHOUT)
    @HandlerR63A(IncomingR63A.ROOM_USER_CHAT, IncomingR63A.ROOM_USER_SHOUT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null) return

        val isR63A = habboSession.release == "R63A"

        val (message, bubble) = parse(habboRequest, isR63A) ?: return
        // todo: check if user can use bubble
        // todo: check if user is muted
        habboSession.roomUser?.chat(
            habboSession.roomUser!!.virtualID,
            message,
            bubble,
            if (!isR63A && habboRequest.incoming == Incoming.ROOM_USER_SHOUT || isR63A && habboRequest.incomingR63A == IncomingR63A.ROOM_USER_SHOUT) ChatType.SHOUT else ChatType.CHAT,
            false
        )
    }

    private fun parse(habboRequest: HabboRequest, isR63A: Boolean): Pair<String, Int>? {
        var message = habboRequest.readUTF().trim()

        if (message.isBlank()) return null
        if (message.length > Byte.MAX_VALUE) message = message.substring(0, Byte.MAX_VALUE.toInt())
        val bubble = if (isR63A) -1 else habboRequest.readInt()

        return Pair(message, bubble)
    }
}