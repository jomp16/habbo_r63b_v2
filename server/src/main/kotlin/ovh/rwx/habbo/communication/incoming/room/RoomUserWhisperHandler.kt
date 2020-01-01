/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.game.room.tasks.ChatType
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUserWhisperHandler {
    @Handler(Incoming.ROOM_USER_WHISPER)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val raw = habboRequest.readUTF()
        val bubble = habboRequest.readInt()
        val targetName = raw.substring(0, raw.indexOf(' '))
        var message = raw.substring(targetName.length + 1)

        if (message.isEmpty()) return
        if (message.length > 100) message = message.substring(0, 100)

        if (habboSession.userInformation.username == targetName) return
        val targetRoomUser = habboSession.currentRoom!!.roomUsers.values.filter { it.habboSession != null }.find { it.habboSession!!.userInformation.username == targetName }
                ?: return

        habboSession.roomUser!!.chat(habboSession.roomUser!!.virtualID, message, bubble, ChatType.WHISPER, true)
        targetRoomUser.chat(habboSession.roomUser!!.virtualID, message, bubble, ChatType.WHISPER, true)
    }
}