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
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.messenger.MessengerFriend

data class MessengerInitData(
    val maxFriends: Int,
    val maxFriendsHC: Int,
    val friends: Collection<MessengerFriend>,
)

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerInitializeResponse {
    @Response(Outgoing.MESSENGER_INIT)
    @ResponseR63A(OutgoingR63A.MESSENGER_INIT)
    fun response(habboResponse: HabboResponse, data: MessengerInitData) {
        val (maxFriends, maxFriendsHC, friends) = data

        habboResponse.apply {
            writeInt(maxFriends) // userFriendLimit
            writeInt(maxFriends) // normalFriendLimit
            writeInt(maxFriendsHC) // extendedFriendLimit

            if (isVersionAtLeast(2010, 4, 7) && isVersionBefore(2016, 7, 26)) {
                writeInt(maxFriendsHC) // evenMoreExtendedFriendLimit (nome real no parser pré-shuffle); sai do init em 2016-07-26
            }

            writeInt(0) // categories count

            if (isVersionBefore(2014, 12, 10)) {
                // Era pré-2014-12-10 (pré-shuffle e 2013): amigos embutidos no próprio init
                // (parser 2013 _SafeStr_2443: 4 ints + categorias LOOP[id|name] + amigos LOOP[full DTO])
                writeInt(friends.size)
                friends.forEach { serialize(it) }

                if (isVersionBefore(2011, 2, 22)) {
                    writeInt(0) // Pager status?
                    writeInt(0) // Extra padding?
                }
            }
        }
    }
}
