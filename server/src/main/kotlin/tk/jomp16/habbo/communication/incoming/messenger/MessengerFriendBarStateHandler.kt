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

package tk.jomp16.habbo.communication.incoming.messenger

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerFriendBarStateHandler {
    @Handler(Incoming.MESSENGER_FRIEND_BAR_STATE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || !habboSession.habboMessenger.initializedMessenger) return

        habboSession.userPreferences.friendBarOpen = habboRequest.readInt() == 1

        val tmp = habboSession.userPreferences.volume.split(',').map { it.toInt() }

        habboSession.sendHabboResponse(Outgoing.USER_SETTINGS,
                                       tmp[0],
                                       tmp[1],
                                       tmp[2],
                                       habboSession.userPreferences.preferOldChat,
                                       habboSession.userPreferences.ignoreRoomInvite,
                                       habboSession.userPreferences.disableCameraFollow,
                                       habboSession.userPreferences.friendBarOpen,
                                       habboSession.userPreferences.chatColor
                                      )
    }
}