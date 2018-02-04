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

package ovh.rwx.habbo.game.room.tasks

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerSaysSomething
import ovh.rwx.habbo.game.room.IRoomTask
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.plugin.event.events.room.RoomUserChatEvent

class UserChatTask(
        private val roomUser: RoomUser,
        private val virtualID: Int,
        private val message: String,
        private val bubble: Int,
        private val type: ChatType,
        private val skipCommands: Boolean
) : IRoomTask {
    override fun executeTask(room: Room) {
        roomUser.idle = false
        val speechEmotion = getSpeechEmotion(message.toUpperCase())

        if (!skipCommands) {
            HabboServer.pluginManager.executeEventAsync(RoomUserChatEvent(room, roomUser, message, bubble, type))

            if (message.startsWith(':')) return
        }
        var filterMessage = message

        room.wordFilter.forEach { filterMessage = filterMessage.replace(it, "bobba") }

        if (room.wiredHandler.triggerWired(WiredTriggerSaysSomething::class, roomUser, filterMessage)) {
            roomUser.habboSession?.sendHabboResponse(Outgoing.ROOM_USER_WHISPER, roomUser.virtualID, filterMessage, speechEmotion, bubble)

            return
        }

        if (type == ChatType.WHISPER) {
            roomUser.habboSession?.sendHabboResponse(Outgoing.ROOM_USER_WHISPER, virtualID, filterMessage, speechEmotion, bubble)
        } else {
            room.roomUsers.values.forEach {
                if (type == ChatType.CHAT && room.roomData.chatMaxDistance > 0 && room.roomGamemap.tileDistance(roomUser.currentVector3.x, roomUser.currentVector3.y, it.currentVector3.x, it.currentVector3.y) <= room.roomData.chatMaxDistance) {
                    it.habboSession?.sendHabboResponse(Outgoing.ROOM_USER_CHAT, virtualID, filterMessage, speechEmotion, bubble)
                } else if (type == ChatType.SHOUT) {
                    it.habboSession?.sendHabboResponse(Outgoing.ROOM_USER_SHOUT, virtualID, filterMessage, speechEmotion, bubble)
                }
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
