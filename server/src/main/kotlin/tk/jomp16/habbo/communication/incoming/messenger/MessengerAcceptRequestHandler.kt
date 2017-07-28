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

package tk.jomp16.habbo.communication.incoming.messenger

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.communication.outgoing.messenger.MessengerFriendUpdateResponse
import tk.jomp16.habbo.database.messenger.MessengerDao
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.game.user.messenger.MessengerFriend
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerAcceptRequestHandler {
    @Handler(Incoming.MESSENGER_ACCEPT_REQUEST)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || !habboSession.habboMessenger.initialized) return
        var amount = habboRequest.readInt()

        if (amount < 0) return
        if (amount > 50) amount = 50
        val friendIds: MutableList<Int> = ArrayList()
        val requestIds: MutableList<Int> = ArrayList()

        repeat(amount) {
            val userId = habboRequest.readInt()

            if (habboSession.habboMessenger.friends.containsKey(userId)) return@repeat
            val messengerRequest = habboSession.habboMessenger.requests.remove(userId) ?: return@repeat

            friendIds += userId
            requestIds += messengerRequest.id
        }

        MessengerDao.removeRequests(requestIds)
        val friends = MessengerDao.addFriends(habboSession.userInformation.id, friendIds)

        habboSession.habboMessenger.friends += friends.associateBy { it.userId }
        habboSession.sendHabboResponse(Outgoing.MESSENGER_FRIEND_UPDATE, friends, MessengerFriendUpdateResponse.MessengerFriendUpdateMode.INSERT)

        friendIds.forEach {
            val friendHabboSession = HabboServer.habboSessionManager.getHabboSessionById(it) ?: return@forEach

            if (!friendHabboSession.habboMessenger.initialized) return@forEach
            val messengerFriend = MessengerFriend(habboSession.userInformation.id)

            friendHabboSession.habboMessenger.friends.put(messengerFriend.userId, messengerFriend)

            friendHabboSession.sendHabboResponse(Outgoing.MESSENGER_FRIEND_UPDATE, listOf(messengerFriend), MessengerFriendUpdateResponse.MessengerFriendUpdateMode.INSERT)
        }
    }
}