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

package ovh.rwx.habbo.communication.outgoing.group

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.incoming.group.GroupMembersHandler
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.group.Group

@Suppress("unused", "UNUSED_PARAMETER")
class GroupMembersResponse {
    private val membersPerPage = 14

    @Response(Outgoing.GROUP_MEMBERS)
    fun handle(habboResponse: HabboResponse, group: Group, members: Collection<GroupMembersHandler.GroupMemberInfo>, isOwner: Boolean, page: Int, searchValue: String, requestType: Int) {
        val chunkedMembers = members.chunked(membersPerPage)

        habboResponse.apply {
            writeInt(group.groupData.id)
            writeUTF(group.groupData.name)
            writeInt(group.groupData.roomId)
            writeUTF(group.groupData.badge)

            writeInt(members.size)

            if (chunkedMembers.isEmpty()) {
                writeInt(0)
            } else {
                chunkedMembers[page].let {
                    writeInt(it.size)

                    it.forEach {
                        writeInt(if (it.rank == 2) 0 else if (it.rank == 1) 1 else if (it.rank == 3) 3 else 2)
                        writeInt(it.userId)
                        writeUTF(it.userName)
                        writeUTF(it.figure)
                        writeUTF(it.createdAt.format(HabboServer.DATE_TIME_FORMATTER_ONLY_DAYS))
                    }
                }
            }

            writeBoolean(isOwner)
            writeInt(membersPerPage)
            writeInt(page)
            writeInt(requestType)
            writeUTF(searchValue)
        }
    }
}