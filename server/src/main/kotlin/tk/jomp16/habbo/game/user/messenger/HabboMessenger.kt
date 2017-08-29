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

package tk.jomp16.habbo.game.user.messenger

import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.communication.outgoing.messenger.MessengerFriendUpdateResponse
import tk.jomp16.habbo.database.messenger.MessengerDao
import tk.jomp16.habbo.database.user.UserInformationDao
import tk.jomp16.habbo.game.user.HabboSession
import java.util.*

class HabboMessenger(private val habboSession: HabboSession) {
    val friends: MutableMap<Int, MessengerFriend> = HashMap()
    val requests: MutableMap<Int, MessengerRequest> = HashMap()
    var initialized: Boolean = false

    internal fun load() {
        if (!initialized) {
            if (habboSession.hasPermission("acc_server_console")) {
                // server console!
                friends += UserInformationDao.serverConsoleUserInformation.id to MessengerFriend(UserInformationDao.serverConsoleUserInformation.id)

                // stub group
                // friends += -1 to MessengerFriend(-1)
            }

            friends += MessengerDao.getFriends(habboSession.userInformation.id).associateBy { it.userId }
            requests += MessengerDao.getRequests(habboSession.userInformation.id).associateBy { it.fromId }

            initialized = true
        }
    }

    fun notifyFriends() {
        friends.values.filter { it.userId > 0 }.filter { it.online && it.habboSession?.habboMessenger?.initialized == true }.forEach {
            it.habboSession!!.sendHabboResponse(Outgoing.MESSENGER_FRIEND_UPDATE, listOf(it.habboSession!!.habboMessenger.friends[habboSession.userInformation.id]), MessengerFriendUpdateResponse.MessengerFriendUpdateMode.UPDATE)
        }
    }
}