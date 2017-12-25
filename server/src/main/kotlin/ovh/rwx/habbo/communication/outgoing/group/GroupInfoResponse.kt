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

package ovh.rwx.habbo.communication.outgoing.group

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.group.Group

@Suppress("unused", "UNUSED_PARAMETER")
class GroupInfoResponse {
    @Response(Outgoing.GROUP_INFO)
    fun handle(habboResponse: HabboResponse, userId: Int, favoriteGroup: Boolean, group: Group, newWindow: Boolean) {
        habboResponse.apply {
            writeInt(group.groupData.id)
            writeBoolean(true) // ???
            writeInt(group.groupData.membershipState.state)
            writeUTF(group.groupData.name)
            writeUTF(group.groupData.description)
            writeUTF(group.groupData.badge)
            writeInt(group.groupData.roomId)
            writeUTF(group.groupData.room.roomData.description)
            writeInt(if (userId == group.groupData.ownerId) 3 else 0) // todo: request status
            writeInt(group.groupMembers.size)
            writeBoolean(favoriteGroup)
            writeUTF(group.groupData.createdAt.format(HabboServer.DATE_TIME_FORMATTER_ONLY_DAYS))
            writeBoolean(userId == group.groupData.ownerId) // is owner
            writeBoolean(false) // todo: is admin
            writeUTF(group.groupData.ownerName)
            writeBoolean(newWindow)
            writeBoolean(group.groupData.onlyAdminCanDecorateRoom)
            writeInt(0) // todo: requests
            writeBoolean(false) // todo: group forum
        }
    }
}