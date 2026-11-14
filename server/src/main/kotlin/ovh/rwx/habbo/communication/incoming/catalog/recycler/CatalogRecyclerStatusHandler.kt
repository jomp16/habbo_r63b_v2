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

package ovh.rwx.habbo.communication.incoming.catalog.recycler

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.catalog.recycler.CatalogRecyclerStatusResponse
import ovh.rwx.habbo.game.catalog.RecyclerSettings
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogRecyclerStatusHandler {
    @Handler(Incoming.CATALOG_RECYCLER_STATUS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        habboSession.sendHabboResponse(Outgoing.CATALOG_RECYCLER_STATUS, currentStatus(), 0)
    }

    @HandlerR63A(IncomingR63A.CATALOG_RECYCLER_STATUS)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        habboSession.sendHabboResponse(OutgoingR63A.CATALOG_RECYCLER_STATUS, currentStatus(), 0)
    }

    // todo: add recycler closed with timeout
    private fun currentStatus(): CatalogRecyclerStatusResponse.CatalogRecyclerStatus =
        if (RecyclerSettings.open) CatalogRecyclerStatusResponse.CatalogRecyclerStatus.OPEN
        else CatalogRecyclerStatusResponse.CatalogRecyclerStatus.CLOSED
}