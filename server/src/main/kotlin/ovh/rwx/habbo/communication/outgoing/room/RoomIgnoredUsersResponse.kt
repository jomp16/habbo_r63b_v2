/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.room

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.ignore.IgnoredUser

/** Lista completa de ignorados, do mais antigo para o mais recente (substitui a lista do cliente). */
data class RoomIgnoredUsersData(val ignoredUsers: List<IgnoredUser>)

@Suppress("unused", "UNUSED_PARAMETER")
class RoomIgnoredUsersResponse {
    @Response(Outgoing.ROOM_IGNORED_USERS)
    @ResponseR63A(OutgoingR63A.ROOM_IGNORED_USERS)
    fun response(habboResponse: HabboResponse, data: RoomIgnoredUsersData) {
        habboResponse.apply {
            writeInt(data.ignoredUsers.size)

            if (isVersionAtLeast(2025, 12, 10)) {
                data.ignoredUsers.forEach { writeInt(it.userId) }
            } else {
                data.ignoredUsers.forEach { writeUTF(it.username) }
            }
        }
    }
}
