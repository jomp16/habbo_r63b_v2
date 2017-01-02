/*
 * Copyright (C) 2017 jomp16
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
import tk.jomp16.habbo.database.messenger.MessengerDao
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.game.user.messenger.MessengerFriend
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerRemoveFriendHandler {
    @Handler(Incoming.MESSENGER_REMOVE_FRIEND)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || !habboSession.habboMessenger.initializedMessenger) return

        var amount = habboRequest.readInt()

        if (amount < 0) return
        if (amount > 100) amount = 100

        val messengerFriends: MutableList<MessengerFriend> = ArrayList()

        (0..amount - 1).forEach {
            val userId = habboRequest.readInt()

            val messengerFriend = habboSession.habboMessenger.friends.remove(userId) ?: return@forEach

            messengerFriends += messengerFriend
        }

        habboSession.sendHabboResponse(Outgoing.MESSENGER_FRIEND_UPDATE, messengerFriends, -1)

        messengerFriends.forEach {
            val friendHabboSession = HabboServer.habboSessionManager.getHabboSessionById(it.userId) ?: return@forEach

            if (!friendHabboSession.habboMessenger.initializedMessenger) return@forEach
            val messengerFriend = friendHabboSession.habboMessenger.friends.remove(
                    habboSession.userInformation.id) ?: return@forEach

            friendHabboSession.sendHabboResponse(Outgoing.MESSENGER_FRIEND_UPDATE, listOf(messengerFriend), -1)
        }

        MessengerDao.removeFriendships(habboSession.userInformation.id, messengerFriends.map { it.userId })
    }
}