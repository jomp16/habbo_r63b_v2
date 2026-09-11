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

package ovh.rwx.habbo.communication.outgoing.room

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.RoomChatMessageBubbles

/**
 * Payload estruturado para mensagens de chat no quarto (ROOM_USER_CHAT, ROOM_USER_SHOUT, ROOM_USER_WHISPER).
 *
 * @param virtualId ID virtual da entidade no quarto.
 * @param message Mensagem falada/gritada/sussurrada.
 * @param emotion Emoção associada (ex: sorriso, raiva).
 * @param bubble Balão de chat (aplicável no R63B+).
 */
data class RoomUserChatData(
    val virtualId: Int,
    val message: String,
    val emotion: Int = 0,
    val bubble: RoomChatMessageBubbles = RoomChatMessageBubbles.NORMAL,
)

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUserChatResponse {
    @Response(Outgoing.ROOM_USER_CHAT, Outgoing.ROOM_USER_SHOUT, Outgoing.ROOM_USER_WHISPER)
    @ResponseR63A(OutgoingR63A.ROOM_USER_CHAT, OutgoingR63A.ROOM_USER_SHOUT, OutgoingR63A.ROOM_USER_WHISPER)
    fun response(habboResponse: HabboResponse, data: RoomUserChatData) {
        habboResponse.apply {
            writeInt(data.virtualId)
            writeUTF(data.message)
            writeInt(data.emotion)
            if (outgoingR63A == null) {
                writeInt(data.bubble.type)
            }
            writeInt(0) // todo: linkRefs / string / string / integer
            writeInt(-1) // trackingId
        }
    }
}