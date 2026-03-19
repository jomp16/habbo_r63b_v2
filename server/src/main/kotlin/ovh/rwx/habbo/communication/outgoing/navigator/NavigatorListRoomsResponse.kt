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

package ovh.rwx.habbo.communication.outgoing.navigator

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.Room

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorListRoomsResponse {
    @ResponseR63A(OutgoingR63A.NAVIGATOR_LIST_ROOMS)
    fun responseR63A(
        habboResponse: HabboResponse,
        categoryId: Int,
        mode: Int,
        query: String,
        rooms: List<Room>,
        showEvents: Boolean
    ) {
        habboResponse.apply {
            writeInt(categoryId) // _searchType
            writeUTF(query) // searchParam
            writeInt(rooms.size)

            rooms.forEach { serialize(it, showEvents, false) }

            writeBoolean(false) // official room
        }
    }
}
