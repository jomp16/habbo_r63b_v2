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
import ovh.rwx.habbo.game.user.HabboSession
import java.time.LocalDateTime

@Suppress("unused", "UNUSED_PARAMETER")
class GroupMembersHandler {
    @Handler(Incoming.GROUP_MEMBERS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val groupId = habboRequest.readInt()
        val page = habboRequest.readInt()
        val searchValue = habboRequest.readUTF()
        val requestType = habboRequest.readInt() // 0 = all, 1 = admins, 2 = requests

        val group = HabboServer.habboGame.groupManager.groups[groupId] ?: return
        val members = when (requestType) {
            1 -> group.admins
                    .filter { if (searchValue.isNotBlank()) it.userName.matches("(?i:.*$searchValue.*)".toRegex()) else true }
                    .map { GroupMemberInfo(it.id, it.userId, it.userName, it.figure, it.rank, it.createdAt) }
            2 -> group.requests
                    .filter { if (searchValue.isNotBlank()) it.userName.matches("(?i:.*$searchValue.*)".toRegex()) else true }
                    .map { GroupMemberInfo(it.id, it.userId, it.userName, it.figure, 3, it.createdAt) }
            else -> group.members
                    .filter { if (searchValue.isNotBlank()) it.userName.matches("(?i:.*$searchValue.*)".toRegex()) else true }
                    .map { GroupMemberInfo(it.id, it.userId, it.userName, it.figure, it.rank, it.createdAt) }
        }

        habboSession.sendHabboResponse(Outgoing.GROUP_MEMBERS, group, members, habboSession.userInformation.id == group.groupData.ownerId, page, searchValue, requestType)
    }

    data class GroupMemberInfo(
            val id: Int,
            val userId: Int,
            val userName: String,
            val figure: String,
            val rank: Int,
            val createdAt: LocalDateTime
    )
}