/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.game.room.tasks

import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.room.IRoomTask
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

class UserChatTask(private val roomUser: RoomUser, private val message: String, private val bubble: Int, private val type: ChatType) : IRoomTask {
    override fun executeTask(room: Room) {
        roomUser.idle = false

        // todo: wired trigger on chat

        room.roomUsers.values.forEach {
            if (type == ChatType.CHAT && (room.roomData.chatMaxDistance > 0 && room.roomGamemap.tileDistance(
                    roomUser.currentVector3.x, roomUser.currentVector3.y, it.currentVector3.x,
                    it.currentVector3.y) <= room.roomData.chatMaxDistance)) {
                it.habboSession?.sendHabboResponse(Outgoing.ROOM_USER_CHAT, roomUser.virtualID, message,
                                                   getSpeechEmotion(message.toUpperCase()), bubble)
            } else if (type == ChatType.SHOUT) {
                it.habboSession?.sendHabboResponse(Outgoing.ROOM_USER_SHOUT, roomUser.virtualID, message,
                                                   getSpeechEmotion(message.toUpperCase()), bubble)
            }
        }
    }
}

private fun getSpeechEmotion(message: String): Int {
    // Happy face
    if (message.contains(":)") ||
            message.contains(";)") ||
            message.contains(":D") ||
            message.contains(";D") ||
            message.contains(":]") ||
            message.contains(";]") ||
            message.contains("=)") ||
            message.contains("=]") ||
            message.contains("=D") ||
            message.contains(":>") ||
            message.contains(":-]") ||
            message.contains(":-)") ||
            message.contains(":-D")) {
        return 1
    }

    // Angry face
    if (message.contains(">:(") ||
            message.contains(">;(") ||
            message.contains(">:[") ||
            message.contains(">;[") ||
            message.contains(">=(") ||
            message.contains(">=[") ||
            message.contains(":@")) {
        return 2
    }

    // Surprised face
    if (message.contains(":O") ||
            message.contains(";O") ||
            message.contains(":0") ||
            message.contains(";0") ||
            message.contains(">:O") ||
            message.contains(">;O") ||
            message.contains(">:0") ||
            message.contains(">;0") ||
            message.contains("=O") ||
            message.contains(">=O")) {
        return 3
    }

    // Sad face
    if (message.contains(":(") ||
            message.contains(":[") ||
            message.contains("=(") ||
            message.contains("=[") ||
            message.contains(":C") ||
            message.contains("=C") ||
            message.contains(":'(") ||
            message.contains(":'[") ||
            message.contains("='(") ||
            message.contains("='[") ||
            message.contains(":'C") ||
            message.contains("='C") ||
            message.contains(":<") ||
            message.contains(":-[") ||
            message.contains(":-(")) {
        return 4
    }

    // Normal face
    return 0
}

enum class ChatType {
    CHAT,
    SHOUT,
    WHISPER
}
