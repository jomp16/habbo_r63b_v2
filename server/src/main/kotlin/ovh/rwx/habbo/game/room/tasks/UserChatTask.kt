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

package ovh.rwx.habbo.game.room.tasks

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerSaysSomething
import ovh.rwx.habbo.game.room.IRoomTask
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomChatMessageBubbles
import ovh.rwx.habbo.game.room.RoomChatType
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.plugin.event.events.room.RoomUserChatEvent
import ovh.rwx.habbo.util.Direction
import java.util.*

class UserChatTask(
    private val roomUser: RoomUser,
    private val virtualID: Int,
    private val message: String,
    private val bubble: RoomChatMessageBubbles,
    private val type: RoomChatType,
    private val skipCommands: Boolean
) : IRoomTask {
    override fun executeTask(room: Room) {
        roomUser.idle = false
        val speechEmotion = getSpeechEmotion(message.uppercase(Locale.getDefault()))

        if (!skipCommands) {
            HabboServer.pluginManager.executeEventAsync(RoomUserChatEvent(room, roomUser, message, bubble, type))

            if (message.startsWith(':')) return
        }
        var filterMessage = message

        room.wordFilter.forEach { filterMessage = filterMessage.replace(it, "bobba") }

        val triggeredWireds = room.wiredHandler.triggerWired(WiredTriggerSaysSomething::class, roomUser, filterMessage)

        if (triggeredWireds.isNotEmpty()) {
            // Verifica se deve esconder a mensagem usando os wireds acionados
            val shouldHide = triggeredWireds
                .filterIsInstance<WiredTriggerSaysSomething>()
                .any { it.shouldHideMessage() }

            if (shouldHide) {
                // Não exibe a mensagem - apenas processa o trigger
                return
            } else {
                // Exibe como whisper quando wired é ativado mas não deve esconder
                roomUser.habboSession?.sendHabboResponse(
                    Outgoing.ROOM_USER_WHISPER,
                    roomUser.virtualID,
                    filterMessage,
                    speechEmotion,
                    bubble
                )
                return
            }
        }

        if (type == RoomChatType.WHISPER) {
            roomUser.habboSession?.sendHabboResponse(
                Outgoing.ROOM_USER_WHISPER,
                virtualID,
                filterMessage,
                speechEmotion,
                bubble
            )
        } else {
            room.roomUsers.values.forEach {
                if (type == RoomChatType.CHAT && room.roomData.chatMaxDistance > 0 && room.roomGamemap.tileDistance(
                        roomUser.currentVector3.x,
                        roomUser.currentVector3.y,
                        it.currentVector3.x,
                        it.currentVector3.y
                    ) <= room.roomData.chatMaxDistance
                ) {
                    it.habboSession?.let { habboSession ->
                        if (habboSession.release != "R63A") {
                            habboSession.sendHabboResponse(
                                Outgoing.ROOM_USER_CHAT,
                                virtualID,
                                filterMessage,
                                speechEmotion,
                                bubble
                            )
                        } else {
                            habboSession.sendHabboResponse(
                                OutgoingR63A.ROOM_USER_CHAT,
                                virtualID,
                                filterMessage,
                                speechEmotion,
                            )
                        }
                    }
                    turnHeadTowardsSpeaker(it, roomUser)
                } else if (type == RoomChatType.SHOUT) {
                    it.habboSession?.let { habboSession ->
                        if (habboSession.release != "R63A") {
                            habboSession.sendHabboResponse(
                                Outgoing.ROOM_USER_SHOUT,
                                virtualID,
                                filterMessage,
                                speechEmotion,
                                bubble
                            )
                        } else {
                            habboSession.sendHabboResponse(
                                OutgoingR63A.ROOM_USER_SHOUT,
                                virtualID,
                                filterMessage,
                                speechEmotion,
                            )
                        }
                    }
                    turnHeadTowardsSpeaker(it, roomUser)
                }
            }
        }
    }

    private fun turnHeadTowardsSpeaker(listener: RoomUser, speaker: RoomUser) {
        if (listener == speaker) return
        if (listener.walking || listener.idle) return
        if (listener.statusMap.containsKey("sit") || listener.statusMap.containsKey("lay")) return

        val targetRotation = Direction.calculate(
            listener.currentVector3.x,
            listener.currentVector3.y,
            speaker.currentVector3.x,
            speaker.currentVector3.y
        )

        if (Direction.rotationDistance(listener.bodyRotation, targetRotation) <= 1) {
            listener.headRotation = targetRotation
            listener.headResetCycle = 4 // 2 segundos (4 ciclos de 500ms)
            listener.updateNeeded = true
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
        message.contains(":-D")
    ) {
        return 1
    }
    // Angry face
    if (message.contains(">:(") ||
        message.contains(">;(") ||
        message.contains(">:[") ||
        message.contains(">;[") ||
        message.contains(">=(") ||
        message.contains(">=[") ||
        message.contains(":@")
    ) {
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
        message.contains(">=O")
    ) {
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
        message.contains(":-(")
    ) {
        return 4
    }
    // Normal face
    return 0
}

