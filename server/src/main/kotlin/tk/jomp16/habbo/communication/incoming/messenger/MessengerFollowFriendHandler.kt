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
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerFollowFriendHandler {
    @Handler(Incoming.MESSENGER_FOLLOW_FRIEND)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || !habboSession.habboMessenger.initialized) return
        val friendId = habboRequest.readInt()

        if (friendId == 0 || friendId == habboSession.userInformation.id) return
        val friendHabboSession = HabboServer.habboSessionManager.getHabboSessionById(friendId)

        if (friendHabboSession?.currentRoom == null) habboSession.sendHabboResponse(Outgoing.MESSENGER_FOLLOW_FRIEND_ERROR, 2)

        if (!habboSession.habboMessenger.friends.containsKey(friendId) && !habboSession.hasPermission("acc_can_follow_anybody")) habboSession.sendHabboResponse(Outgoing.MESSENGER_FOLLOW_FRIEND_ERROR, 0)

        habboSession.sendHabboResponse(Outgoing.ROOM_FORWARD, friendHabboSession!!.currentRoom!!.roomData.id)
    }
}