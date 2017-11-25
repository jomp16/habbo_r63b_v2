/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.game.user.messenger.MessengerFriend

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerFriendUpdateResponse {
    @Response(Outgoing.MESSENGER_FRIEND_UPDATE)
    fun response(habboResponse: HabboResponse, messengerFriends: Collection<MessengerFriend>, mode: MessengerFriendUpdateMode) {
        habboResponse.apply {
            // todo: update categories
            writeInt(0) // size to update messenger category
            writeInt(messengerFriends.size)

            messengerFriends.forEach {
                writeInt(mode.mode)

                if (mode == MessengerFriendUpdateMode.REMOVE) writeInt(it.userId)
                else serialize(it)
            }
        }
    }

    enum class MessengerFriendUpdateMode(val mode: Int) {
        INSERT(1),
        UPDATE(0),
        REMOVE(-1)
    }
}