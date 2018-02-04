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

package ovh.rwx.habbo.communication.incoming.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.catalog.CatalogItem
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogPurchaseHandler {
    @Handler(Incoming.CATALOG_PURCHASE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return
        val pageId = habboRequest.readInt()
        val itemId = habboRequest.readInt()
        val extraData = habboRequest.readUTF()
        var amount = habboRequest.readInt()
        val catalogPage = HabboServer.habboGame.catalogManager.catalogPages.find { it.id == pageId }

        if (catalogPage != null && (!catalogPage.enabled || !catalogPage.visible || habboSession.userInformation.rank < catalogPage.minRank || catalogPage.clubOnly && !habboSession.habboSubscription.validUserSubscription)) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)

            return
        }

        if (catalogPage?.pageLayout == "vip_buy") {
            // purchase HC
            HabboServer.habboGame.catalogManager.purchaseHC(habboSession, itemId)

            return
        }
        val catalogItem: CatalogItem? = HabboServer.habboGame.catalogManager.catalogItems.find { it.id == itemId }

        if (catalogItem == null) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)

            return
        }

        if (amount < 1 || amount > 1 && catalogItem.limited) amount = 1
        else if (amount > 100) amount = 100

        HabboServer.habboGame.catalogManager.purchase(habboSession, catalogItem, extraData, amount)
    }
}