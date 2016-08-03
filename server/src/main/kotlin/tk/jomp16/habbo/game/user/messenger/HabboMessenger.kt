/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.game.user.messenger

import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.messenger.MessengerDao
import tk.jomp16.habbo.game.user.HabboSession
import java.util.*

class HabboMessenger(private val habboSession: HabboSession) {
    val friends: MutableMap<Int, MessengerFriend> = HashMap(
            MessengerDao.getFriends(habboSession.userInformation.id).associateBy { it.userId })
    val requests: MutableMap<Int, MessengerRequest> = HashMap(
            MessengerDao.getRequests(habboSession.userInformation.id).associateBy { it.fromId })

    var initializedMessenger: Boolean = false

    init {
        if (habboSession.userInformation.rank >= 7) {
            // server console!
            friends += Int.MAX_VALUE to MessengerFriend(Int.MAX_VALUE)
        }
    }
    fun notifyFriends() {
        friends.values.filter {
            it.online && it.habboSession?.habboMessenger?.initializedMessenger ?: false
        }.forEach {
            it.habboSession?.sendHabboResponse(Outgoing.MESSENGER_FRIEND_UPDATE,
                                               listOf(it.habboSession!!.habboMessenger.friends[habboSession.userInformation.id]),
                                               0)
        }
    }
}