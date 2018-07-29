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
import ovh.rwx.habbo.game.group.GroupMembershipState
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class GroupUpdateSettingsHandler {
    @Handler(Incoming.GROUP_UPDATE_SETTINGS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        val groupId = habboRequest.readInt()
        val group = HabboServer.habboGame.groupManager.groups[groupId] ?: return

        if (group.admins.singleOrNull { it.userId == habboSession.userInformation.id } == null || !habboSession.hasPermission("acc_any_group_admin")) return

        val membershipState = GroupMembershipState.valueOf(habboRequest.readInt())
        val onlyAdminCanDecorateRoom = habboRequest.readInt() == 1

        println("membershipState=$membershipState")
        println("onlyAdminCanDecorateRoom=$onlyAdminCanDecorateRoom")
        // Optimization: if both are the same, don't update and send update request to client.
        if (group.groupData.membershipState == membershipState && group.groupData.onlyAdminCanDecorateRoom == onlyAdminCanDecorateRoom) return

        group.groupData.membershipState = membershipState
        group.groupData.onlyAdminCanDecorateRoom = onlyAdminCanDecorateRoom

        group.groupData.room.updateGroupInfo()
        group.groupData.room.updateGroupRights()

        if (group.groupData.roomId != habboSession.currentRoom?.roomData?.id) {
            habboSession.currentRoom?.updateGroupInfo()
            habboSession.currentRoom?.updateGroupRights()
        }
    }
}