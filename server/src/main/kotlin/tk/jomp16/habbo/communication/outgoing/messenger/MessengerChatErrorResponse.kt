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

package tk.jomp16.habbo.communication.outgoing.messenger

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerChatErrorResponse {
    @Response(Outgoing.MESSENGER_CHAT_ERROR)
    fun handle(habboResponse: HabboResponse, messengerChatError: MessengerChatError, userId: Int, message: String) {
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