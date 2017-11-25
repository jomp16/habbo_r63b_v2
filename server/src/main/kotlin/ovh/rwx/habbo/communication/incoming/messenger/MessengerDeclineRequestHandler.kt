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

package ovh.rwx.habbo.communication.incoming.messenger

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.database.messenger.MessengerDao
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerDeclineRequestHandler {
    @Handler(Incoming.MESSENGER_DECLINE_REQUEST)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || !habboSession.habboMessenger.initialized) return
        val declineAll = habboRequest.readBoolean()
        var amount = habboRequest.readInt()

        if (amount < 0) return
        if (amount > 50) amount = 50

        if (declineAll) {
            habboSession.habboMessenger.requests.clear()

            MessengerDao.removeAllRequests(habboSession.userInformation.id)

            return
        }
        val ids: MutableList<Int> = mutableListOf()

        repeat(amount) {
            val userId = habboRequest.readInt()
            val messengerRequest = habboSession.habboMessenger.requests.remove(userId) ?: return@repeat

            ids += messengerRequest.id
        }

        MessengerDao.removeRequests(ids)
    }
}