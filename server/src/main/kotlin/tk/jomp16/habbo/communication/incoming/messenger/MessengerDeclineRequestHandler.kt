/*
 * Copyright (C) 2016 jomp16
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

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.database.messenger.MessengerDao
import tk.jomp16.habbo.game.user.HabboSession
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerDeclineRequestHandler {
    @Handler(Incoming.MESSENGER_DECLINE_REQUEST)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || !habboSession.habboMessenger.initializedMessenger) return

        val declineAll = habboRequest.readBoolean()
        var amount = habboRequest.readInt()

        if (amount < 0) return
        if (amount > 50) amount = 50

        if (declineAll) {
            habboSession.habboMessenger.requests.clear()

            MessengerDao.removeAllRequests(habboSession.userInformation.id)

            return
        }

        val ids: MutableList<Int> = ArrayList()

        (0..amount - 1).forEach {
            val userId = habboRequest.readInt()

            val messengerRequest = habboSession.habboMessenger.requests.remove(userId) ?: return@forEach

            ids += messengerRequest.id
        }

        MessengerDao.removeRequests(ids)
    }
}