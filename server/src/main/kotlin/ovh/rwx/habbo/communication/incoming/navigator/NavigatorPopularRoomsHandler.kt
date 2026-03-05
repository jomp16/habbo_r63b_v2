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

package ovh.rwx.habbo.communication.incoming.navigator

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorPopularRoomsHandler {
    @HandlerR63A(IncomingR63A.NAVIGATOR_POPULAR_ROOMS)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        val categoryId = habboRequest.readUTF().toIntOrNull() ?: -1

        println(categoryId)

        val rooms =
            if (categoryId == -1) HabboServer.habboGame.roomManager.rooms.values.filter { it.roomTask != null && it.userManager.users.isNotEmpty() }
                .sortedBy { it.userManager.users.size }.take(8)
            else HabboServer.habboGame.roomManager.rooms.values.filter { it.roomTask != null && it.userManager.users.isNotEmpty() && it.roomData.category == categoryId }
                .sortedBy { it.userManager.users.size }.take(8)

        habboSession.sendHabboResponse(
            OutgoingR63A.NAVIGATOR_LIST_ROOMS,
            categoryId,
            1,
            "",
            rooms,
            false
        )
    }
}
