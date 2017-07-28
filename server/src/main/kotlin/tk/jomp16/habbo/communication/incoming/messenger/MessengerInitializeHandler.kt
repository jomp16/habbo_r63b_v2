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

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.messenger.MessengerDao
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerInitializeHandler {
    @Handler(Incoming.MESSENGER_INIT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.habboMessenger.initialized) return

        habboSession.habboMessenger.load()

        habboSession.sendHabboResponse(Outgoing.MESSENGER_INIT, 2000 // max friends
        ) // MessengerInitComposer
        habboSession.sendHabboResponse(Outgoing.MESSENGER_FRIENDS, habboSession.habboMessenger.friends.values) // BuddyListComposer
        MessengerDao.getOfflineMessages(habboSession.userInformation.id).forEach {
            habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, it.first, it.second, it.third)
        }

        habboSession.habboMessenger.initialized = true

        habboSession.habboMessenger.notifyFriends()
    }
}