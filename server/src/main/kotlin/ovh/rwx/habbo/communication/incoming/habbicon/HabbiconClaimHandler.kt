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

package ovh.rwx.habbo.communication.incoming.habbicon

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogPurchaseOkData
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class HabbiconClaimHandler {
    @Handler(Incoming.CLAIM_HABBICON)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val habbiconId = habboRequest.readInt()
        if (habbiconId <= 0) return

        val success = HabboServer.habboGame.habbiconManager.claimHabbicon(habboSession, habbiconId)
        if (success) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_OK, CatalogPurchaseOkData())
        } else {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)
        }
    }
}

