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

package ovh.rwx.habbo.communication.outgoing.messenger

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerChatErrorResponse {
    @Response(Outgoing.MESSENGER_CHAT_ERROR)
    fun response(habboResponse: HabboResponse, messengerChatError: MessengerChatError, userId: Int, message: String) {
        habboResponse.apply {
            writeInt(messengerChatError.errorCode)
            writeInt(userId)
            writeUTF(message)
        }
    }

    enum class MessengerChatError(val errorCode: Int) {
        FRIEND_MUTED(3),
        SENDER_MUTED(4),
        FRIEND_OFFLINE(5),
        NOT_FRIENDS(6),
        FRIEND_BUSY(7),
        FRIEND_HAS_NO_CHAT(8),
        SENDER_HAS_NO_CHAT(9),
        OFFLINE_MESSAGE_FAILED(10)
    }
}