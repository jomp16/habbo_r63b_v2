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

package ovh.rwx.habbo.communication.incoming.group

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.group.GroupJoinErrorResponse
import ovh.rwx.habbo.game.group.GroupMembershipState
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class GroupRequestJoinHandler {
    @Handler(Incoming.GROUP_REQUEST_JOIN)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val groupId = habboRequest.readInt()
        val group = HabboServer.habboGame.groupManager.groups[groupId] ?: return

        if (habboSession.groups.size >= 50 && !habboSession.habboSubscription.validUserSubscription) {
            // Seems Habbo only allows a user to join to 50 groups max non HC
            habboSession.sendHabboResponse(Outgoing.GROUP_JOIN_ERROR, GroupJoinErrorResponse.GroupJoinError.NOT_HC_LIMIT_PURCHASE)

            return
        }

        when (group.groupData.membershipState) {
            GroupMembershipState.OPEN -> {
                // Add user to members right now
            }
            GroupMembershipState.ADMIN_APPROVAL -> {
                // Add group request and send it to the admins
            }
            else -> habboSession.sendHabboResponse(Outgoing.GROUP_JOIN_ERROR, GroupJoinErrorResponse.GroupJoinError.NOT_ACCEPTING_REQUEST)
        }
    }
}