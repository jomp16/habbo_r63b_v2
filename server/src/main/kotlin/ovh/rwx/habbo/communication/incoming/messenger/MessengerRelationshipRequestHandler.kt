/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerFriendUpdateResponse
import ovh.rwx.habbo.database.messenger.MessengerDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.messenger.MessengerRelationship

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerRelationshipRequestHandler {
    @Handler(Incoming.MESSENGER_SET_RELATIONSHIP)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || !habboSession.habboMessenger.initialized) return
        val friendId = habboRequest.readInt()
        var relationship = habboRequest.readInt()

        if (relationship < 0 || relationship > 3) relationship = 0

        val messengerFriend = habboSession.habboMessenger.friends[friendId] ?: return
        messengerFriend.relationship = MessengerRelationship.findByType(relationship)

        habboSession.sendHabboResponse(Outgoing.MESSENGER_FRIEND_UPDATE, listOf(messengerFriend), MessengerFriendUpdateResponse.MessengerFriendUpdateMode.UPDATE)

        MessengerDao.updateRelationship(messengerFriend)
    }
}