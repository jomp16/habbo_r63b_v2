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
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.game.room.RoomChatMessageBubbles
import ovh.rwx.habbo.game.room.RoomChatType
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUserWhisperHandler {
    @Handler(Incoming.ROOM_USER_WHISPER)
    @HandlerR63A(IncomingR63A.ROOM_USER_WHISPER)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null) return

        val isR63A = habboSession.release == "R63A"

        val (targetName, message, bubble) = parse(habboRequest, isR63A) ?: return

        if (habboSession.userInformation.username == targetName) return
        val targetRoomUser = habboSession.currentRoom!!.userManager.users.values.filter { it.habboSession != null }
            .find { it.habboSession!!.userInformation.username == targetName }
                ?: return

        habboSession.roomUser!!.chat(habboSession.roomUser!!.virtualID, message, bubble, RoomChatType.WHISPER, true)
        targetRoomUser.chat(habboSession.roomUser!!.virtualID, message, bubble, RoomChatType.WHISPER, true)
    }

    private fun parse(habboRequest: HabboRequest, isR63A: Boolean): Triple<String, String, RoomChatMessageBubbles>? {
        var raw = habboRequest.readUTF().trim()

        if (raw.isBlank()) return null
        if (raw.length > Byte.MAX_VALUE) raw = raw.substring(0, Byte.MAX_VALUE.toInt())
        val bubble =
            if (isR63A) RoomChatMessageBubbles.NORMAL else RoomChatMessageBubbles.fromType(habboRequest.readInt())

        val targetName = raw.substringBefore(' ')
        val message = raw.substring(targetName.length + 1)

        return Triple(targetName, message, bubble)
    }
}