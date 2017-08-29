/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.communication.incoming.catalog

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.communication.outgoing.catalog.CatalogOpenRecyclerResultResponse
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogOpenRecyclerHandler {
    @Handler(Incoming.CATALOG_OPEN_RECYCLER)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        // todo: add recycler closed with timeout
        val result = if (HabboServer.habboConfig.recyclerConfig.open) CatalogOpenRecyclerResultResponse.CatalogOpenRecyclerResult.RECYCLER_OPEN
        else CatalogOpenRecyclerResultResponse.CatalogOpenRecyclerResult.RECYCLER_CLOSED

        habboSession.sendHabboResponse(Outgoing.CATALOG_OPEN_RECYCLER_RESULT, result, 0)
    }
}