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
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.navigator.NavigatorGuestRoomsMode
import ovh.rwx.habbo.game.room.navigator.NavigatorListPayload
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorPopularRoomsHandler {
    // 2013: _-2Lm@3559 (String categoria, int) — body compatível (UTF + int ignorado)
    @Handler(Incoming.NAVIGATOR_POPULAR_ROOMS)
    @HandlerR63A(IncomingR63A.NAVIGATOR_POPULAR_ROOMS)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        val categoryId = habboRequest.readUTF().toIntOrNull() ?: -1
        // 2013 manda trailing int (slot do dropdown); bytes não lidos são descartados com o frame
        val popularRooms = HabboServer.habboGame.roomManager.getPopularRooms(categoryId)

        habboSession.sendResponse(
            Outgoing.NAVIGATOR_LIST_ROOMS, OutgoingR63A.NAVIGATOR_LIST_ROOMS,
            NavigatorListPayload(
                rooms = popularRooms,
                mode = NavigatorGuestRoomsMode.POPULAR,
                categoryId = categoryId,
                forceDisplay = if (categoryId == -1) 1 else 2
            )
        )
    }
}
