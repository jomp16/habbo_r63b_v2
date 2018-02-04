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
import ovh.rwx.habbo.database.messenger.MessengerDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.database.user.UserPreferencesDao
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerRequestFriendHandler {
    @Handler(Incoming.MESSENGER_REQUEST_FRIEND)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || !habboSession.habboMessenger.initialized) return
        val username = habboRequest.readUTF()

        if (username.isBlank()) return
        val userInformation = UserInformationDao.getUserInformationByUsername(username) ?: return
        val userPreferences = UserPreferencesDao.getUserPreferences(userInformation.id)

        if (userPreferences.blockNewFriends) return
        val messengerRequest = MessengerDao.addRequest(habboSession.userInformation.id, userInformation.id)
        val friendHabboSession = HabboServer.habboSessionManager.getHabboSessionById(userInformation.id) ?: return

        if (!friendHabboSession.habboMessenger.initialized) return

        friendHabboSession.habboMessenger.requests[messengerRequest.fromId] = messengerRequest
        friendHabboSession.sendHabboResponse(Outgoing.MESSENGER_REQUEST_FRIEND, messengerRequest)
    }
}