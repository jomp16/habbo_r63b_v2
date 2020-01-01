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

package ovh.rwx.habbo.communication.incoming.messenger

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerFriendUpdateResponse
import ovh.rwx.habbo.database.messenger.MessengerDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.messenger.MessengerFriend

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerRemoveFriendHandler {
    @Handler(Incoming.MESSENGER_REMOVE_FRIEND)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.habboMessenger.initialized) return
        var amount = habboRequest.readInt()

        if (amount < 0) return
        if (amount > 100) amount = 100
        val messengerFriends: MutableList<MessengerFriend> = mutableListOf()

        repeat(amount) {
            val userId = habboRequest.readInt()
            val messengerFriend = habboSession.habboMessenger.friends.remove(userId) ?: return@repeat

            messengerFriends += messengerFriend
        }

        habboSession.sendHabboResponse(Outgoing.MESSENGER_FRIEND_UPDATE, messengerFriends, MessengerFriendUpdateResponse.MessengerFriendUpdateMode.REMOVE)

        messengerFriends.forEach {
            val friendHabboSession = HabboServer.habboSessionManager.getHabboSessionById(it.userId) ?: return@forEach

            if (!friendHabboSession.habboMessenger.initialized) return@forEach
            val messengerFriend = friendHabboSession.habboMessenger.friends.remove(habboSession.userInformation.id)
                    ?: return@forEach

            friendHabboSession.sendHabboResponse(Outgoing.MESSENGER_FRIEND_UPDATE, listOf(messengerFriend), MessengerFriendUpdateResponse.MessengerFriendUpdateMode.REMOVE)
        }

        MessengerDao.removeFriendships(habboSession.userInformation.id, messengerFriends.map { it.userId })
    }
}