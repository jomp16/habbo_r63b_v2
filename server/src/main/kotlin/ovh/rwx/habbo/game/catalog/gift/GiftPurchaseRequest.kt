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

package ovh.rwx.habbo.game.catalog.gift

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HabboVersion

/** PurchaseFromCatalogAsGift (cliente -> servidor), já lido do wire. */
data class GiftPurchaseRequest(
    val pageId: Int,
    val offerId: Int,
    val extraData: String,
    val receiverName: String,
    val message: String,
    val spriteId: Int,
    val boxId: Int,
    val ribbonId: Int,
    val showPurchaserName: Boolean,
) {
    companion object {
        /** `showPurchaserName` nasceu em 2011-12-13; antes o presente sempre mostrava o remetente. */
        fun read(request: HabboRequest, version: HabboVersion): GiftPurchaseRequest {
            val pageId = request.readInt()
            val offerId = request.readInt()
            val extraData = request.readUTF()
            val receiverName = request.readUTF()
            val message = request.readUTF()
            val spriteId = request.readInt()
            val boxId = request.readInt()
            val ribbonId = request.readInt()
            val showPurchaserName =
                if (version.isVersionAtLeast(2011, 12, 13)) request.readBoolean() else true

            return GiftPurchaseRequest(
                pageId, offerId, extraData, receiverName, message, spriteId, boxId, ribbonId, showPurchaserName
            )
        }
    }
}
