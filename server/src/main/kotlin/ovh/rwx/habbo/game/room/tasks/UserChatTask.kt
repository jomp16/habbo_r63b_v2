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
import ovh.rwx.habbo.communication.outgoing.room.RoomUserChatData
import ovh.rwx.habbo.game.item.wired.trigger.SayTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerSaysSomething
import ovh.rwx.habbo.game.pet.PetTrick
import ovh.rwx.habbo.game.room.IRoomTask
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomChatMessageBubbles
import ovh.rwx.habbo.game.room.RoomChatType
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomPet
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.containsAny
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

        if (skipCommands) {
            // Se skipCommands é true, apenas enviamos o pacote para os usuários
            // e ignoramos Wireds, Filtros e Plugins para evitar loop.
            broadcastMessage(room, filterMessage = message, speechEmotion = speechEmotion)
            return
        }

        HabboServer.pluginManager.executeEventAsync(RoomUserChatEvent(room, roomUser, message, bubble, type))

        if (message.startsWith(':')) return

        var filterMessage = message

        room.wordFilter.forEach { filterMessage = filterMessage.replace(it, "bobba") }

        val triggeredWireds =
            room.itemManager.wiredHandler.triggerWired(
                WiredTriggerSaysSomething::class, roomUser,
                SayTriggerData(filterMessage)
            )

        if (triggeredWireds.isNotEmpty()) {
            val shouldHide =
                triggeredWireds.filterIsInstance<WiredTriggerSaysSomething>().any { it.shouldHideMessage() }
            if (shouldHide) return

            // Se o wired não esconde, ele vira um sussurro privado por padrão em muitas builds
            roomUser.habboSession.let {
                sendResponse(
                    it,
                    RoomChatType.WHISPER,
                    roomUser.virtualID,
                    filterMessage,
                    speechEmotion,
                    bubble
                )
            }
            return
        }

        broadcastMessage(room, filterMessage, speechEmotion)

        // Pet command detection: check if message matches "<petName> <trickCommand>"
        if (type != RoomChatType.WHISPER) {
            processPetCommands(room, filterMessage)
            HabboServer.habboGame.achievementManager.progress(roomUser.habboSession, "ACH_Tutorial3", 1, false)
        }
    }

    /**
     * Envia a resposta de chat baseada na versão do protocolo do cliente (R63A ou R63B/v2)
     */
    private fun sendResponse(
        habboSession: HabboSession,
        chatType: RoomChatType,
        virtualId: Int,
        message: String,
        emotion: Int,
        bubble: RoomChatMessageBubbles
    ) {
        val (outgoing, outgoingR63A) = when (chatType) {
            RoomChatType.WHISPER -> Outgoing.ROOM_USER_WHISPER to OutgoingR63A.ROOM_USER_WHISPER
            RoomChatType.SHOUT -> Outgoing.ROOM_USER_SHOUT to OutgoingR63A.ROOM_USER_SHOUT
            else -> Outgoing.ROOM_USER_CHAT to OutgoingR63A.ROOM_USER_CHAT
        }

        habboSession.sendResponse(
            outgoing,
            outgoingR63A,
            RoomUserChatData(
                virtualId = virtualId,
                message = message,
                emotion = emotion,
                bubble = bubble
            )
        )
    }

    private fun turnHeadTowardsSpeaker(listener: RoomEntity, speaker: RoomUser) {
        val listenerUser = listener as? RoomUser ?: return
        if (listener == speaker || listener.walking || listenerUser.idle || listener.kicked) return
        if (listener.statusMap.containsKey("sit") || listener.statusMap.containsKey("lay")) return

        val targetRotation = Direction.calculate(
            listener.currentVector3.x,
            listener.currentVector3.y,
            speaker.currentVector3.x,
            speaker.currentVector3.y
        )

        if (Direction.rotationDistance(listener.bodyRotation, targetRotation) <= 1) {
            listenerUser.headRotation = targetRotation
            listenerUser.headResetTick = 4 // 2 segundos (4 ciclos de 500ms)
            listenerUser.updateNeeded = true
        }
    }

    private fun broadcastMessage(room: Room, filterMessage: String, speechEmotion: Int) {
        if (type == RoomChatType.WHISPER) {
            sendResponse(roomUser.habboSession, type, virtualID, filterMessage, speechEmotion, bubble)
        } else {
            room.userManager.entities.values.forEach { targetUser ->
                // Adicionado check para não enviar para quem foi desconectado/kicked no meio do loop
                if (targetUser.kicked) return@forEach

                val canHear = when (type) {
                    RoomChatType.CHAT -> room.roomData.chatMaxDistance <= 0 ||
                            room.roomGamemap.tileDistance(
                                roomUser.currentVector3.x, roomUser.currentVector3.y,
                                targetUser.currentVector3.x, targetUser.currentVector3.y
                            ) <= room.roomData.chatMaxDistance

                    RoomChatType.SHOUT -> true
                }

                if (canHear) {
                    (targetUser as? RoomUser)?.habboSession?.let {
                        sendResponse(it, type, virtualID, filterMessage, speechEmotion, bubble)
                    }
                    turnHeadTowardsSpeaker(targetUser, roomUser)
                }
            }
        }
    }

    private fun processPetCommands(room: Room, message: String) {
        val ownerId = roomUser.habboSession.userInformation.id

        room.userManager.entities.values.filterIsInstance<RoomPet>().forEach { pet ->
            if (pet.petData.userId != ownerId) return@forEach

            val prefix = pet.petData.name + " "
            if (!message.startsWith(prefix, ignoreCase = true)) return@forEach

            val command = message.substring(prefix.length).trim()
            val trick = PetTrick.fromCommand(command) ?: return@forEach

            pet.ai.handleCommand(trick).forEach(pet::executeAction)
        }
    }
}

private fun getSpeechEmotion(message: String): Int {
    return when {
        message.containsAny(":)", ";)", ":D", ";D", "[:", "]=D", ":-)") -> 1
        message.containsAny(">:(", ">:[", ":@", ">=(") -> 2
        message.containsAny(":O", ":0", "O_O", "o.O") -> 3
        message.containsAny(":(", ":[", "=(", "='(", ":<") -> 4
        else -> 0
    }
}
