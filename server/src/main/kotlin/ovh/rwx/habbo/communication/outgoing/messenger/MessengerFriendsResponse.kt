/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.messenger.MessengerFriend

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerFriendsResponse {
    @Response(Outgoing.MESSENGER_FRIENDS)
    fun response(habboResponse: HabboResponse, friends: Collection<MessengerFriend>) {
        habboResponse.apply {
            writeInt(1) // total fragments
            writeInt(0) // fragmentNo
            writeInt(friends.size) // count friendFragment

            friends.forEach { serialize(it) }
        }
    }

    @ResponseR63A(OutgoingR63A.MESSENGER_FRIENDS)
    fun responseR63A(habboResponse: HabboResponse, maxFriends: Int, maxFriendsHC: Int, messengerFriends: Collection<MessengerFriend>) {
        habboResponse.apply {
            writeInt(maxFriends) // Max friends normal
            writeInt(300)
            writeInt(maxFriendsHC) // Max friends HC
            writeInt(maxFriendsHC) // Max friends VIP
            writeInt(0) // category count
            // category structure:
            // int - id
            // string - name
            // groups category
//            writeInt(1)
//            writeUTF("Groups")

            writeInt(messengerFriends.size)

            messengerFriends.forEach { serialize(it) }
        }
    }
}