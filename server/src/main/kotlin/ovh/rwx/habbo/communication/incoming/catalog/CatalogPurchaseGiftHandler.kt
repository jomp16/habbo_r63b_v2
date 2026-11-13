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

package ovh.rwx.habbo.communication.incoming.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.catalog.gift.GiftPurchaseRequest
import ovh.rwx.habbo.game.user.HabboSession

/**
 * PurchaseFromCatalogAsGift. R63A e builds < 2011-12-13: 8 campos; a partir de 2011-12-13 +Boolean showPurchaserName
 * (ver [GiftPurchaseRequest.read]).
 */
@Suppress("unused", "UNUSED_PARAMETER")
class CatalogPurchaseGiftHandler {
    @Handler(Incoming.CATALOG_PURCHASE_GIFT)
    @HandlerR63A(IncomingR63A.CATALOG_PURCHASE_GIFT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val request = GiftPurchaseRequest.read(habboRequest, habboSession.habboVersion)
        val catalogManager = HabboServer.habboGame.catalogManager

        val page = catalogManager.getPage(request.pageId)
        if (page == null || !page.enabled || !page.visible
            || habboSession.userInformation.rank < page.minRank
            || page.clubOnly && !habboSession.habboSubscription.validUserSubscription
        ) {
            habboSession.sendResponse(Outgoing.CATALOG_PURCHASE_ERROR, OutgoingR63A.CATALOG_PURCHASE_ERROR, 0)
            return
        }

        catalogManager.giftService.purchaseGift(habboSession, request)
    }
}
