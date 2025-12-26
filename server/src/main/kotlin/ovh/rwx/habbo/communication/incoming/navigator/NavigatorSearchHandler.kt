/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.incoming.navigator

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.RoomType
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorSearchHandler {
    @Handler(Incoming.NAVIGATOR_SEARCH)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val category = habboRequest.readUTF()
        val searchTerm = habboRequest.readUTF()

        habboSession.sendHabboResponse(Outgoing.NAVIGATOR_SEARCH, habboSession, category, searchTerm)
    }

    @HandlerR63A(IncomingR63A.NAVIGATOR_SEARCH)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        val searchTerm = habboRequest.readUTF()

        val rooms = HabboServer.habboGame.roomManager.rooms.values
            .filter { room ->
                if (room.roomData.roomType == RoomType.PRIVATE) {
                    if (searchTerm.startsWith("owner:")) {
                        return@filter room.roomData.ownerName == searchTerm.substring(6)
                    }

                    val regex = "(?i:$searchTerm.*)".toRegex()

                    if (room.roomData.ownerName.matches(regex)) return@filter true
                    if (room.roomData.name.matches(regex)) return@filter true
                    if (room.roomData.description.matches(regex)) return@filter true

                    for (tag in room.roomData.tags) {
                        if (tag.matches(regex)) return@filter true
                    }
                }
                false
            }
            .sortedBy { it.roomUsers.size }
            .take(50)

        habboSession.sendHabboResponse(
            OutgoingR63A.NAVIGATOR_LIST_ROOMS,
            1, // category
            9, // mode
            searchTerm, // search term
            rooms,
            false // showEvents
        )
    }
}
