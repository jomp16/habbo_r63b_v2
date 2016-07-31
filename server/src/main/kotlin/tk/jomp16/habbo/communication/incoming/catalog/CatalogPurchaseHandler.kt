/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.communication.incoming.catalog

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogPurchaseHandler {
    @Handler(Incoming.CATALOG_PURCHASE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        val pageId = habboRequest.readInt()
        val itemId = habboRequest.readInt()
        val extraData = habboRequest.readUTF()
        var amount = habboRequest.readInt()

        val catalogPage = HabboServer.habboGame.catalogManager.catalogPages.find { it.id == pageId } ?: return
        val catalogItem = catalogPage.catalogItems.find { it.id == itemId } ?: return

        if (amount < 1 || amount > 1 && catalogItem.limited) amount = 1
        else if (amount > 100) amount = 100

        HabboServer.habboGame.catalogManager.purchase(habboSession, catalogPage, catalogItem, extraData, amount)
    }
}