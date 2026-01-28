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

package ovh.rwx.habbo.game.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.ItemType
import java.util.concurrent.atomic.AtomicInteger

data class CatalogItem(
    val id: Int,
    val pageId: Int,
    val itemName: String,
    val orderNum: Int,
    val dealId: Int,
    val catalogName: String,
    val badge: String,
    val costCredits: Int,
    val costPixels: Int,
    val costVip: Int,
    val amount: Int,
    val clubOnly: Boolean,
    val limitedSells: AtomicInteger,
    val limitedTotal: Int,
    val offerActive: Boolean
) : IHabboResponseSerialize {
    val furnishing: Furnishing
        get() = HabboServer.habboGame.itemManager.furnishings[itemName]!!
    val deal: CatalogDeal?
        get() = HabboServer.habboGame.catalogManager.catalogDeals.find { it.id == dealId }
    val offerId: Int
        get() = furnishing.offerId
    val limited = limitedTotal > 0

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        val isHabboAir = params.isNotEmpty() && params[0] as? Boolean == true
        val clientDoesNotSupportSilverCoins = params.isNotEmpty() && params[1] as? Boolean == true

        habboResponse.apply {
            writeInt(id) // offerId
            writeUTF(if (catalogName.isNotBlank() || dealId > 0) catalogName else furnishing.itemName) // localizationId
            writeBoolean(false) // isRent
            writeInt(costCredits) // priceInCredits
            writeInt(costPixels) // priceInActivityPoints
            writeInt(if (costVip > 0) 5 else 0) // activityPointType

            if (!clientDoesNotSupportSilverCoins) {
                writeInt(costVip) // priceInSilver
            }

            writeBoolean(dealId > 0 || furnishing.canGift) // giftable

            // products count and array
            writeInt(if (dealId > 0) deal!!.furnishings.size else 1 + if (badge.isNotBlank()) 1 else 0)

            if (badge.isNotBlank()) {
                writeUTF("b")
                writeUTF(badge)
                if (isHabboAir) writeInt(1)
            }

            if (dealId > 0) {
                deal!!.let { deal ->
                    deal.furnishings.forEachIndexed { i, furnishing ->
                        serializeItem(habboResponse, furnishing, deal.amounts[i])
                    }
                }
            } else {
                serializeItem(habboResponse, furnishing, amount)
            }

            // Campos que vêm APÓS o array de produtos
            writeInt(if (clubOnly) 1 else 0) // clubLevel
            writeBoolean(offerActive && !limited) // bundlePurchaseAllowed
            writeBoolean(false) // isPet ?
            writeUTF("") // previewImage
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(id)
            writeUTF(if (catalogName.isNotBlank() || dealId > 0) catalogName else furnishing.itemName)
            writeInt(costCredits)
            writeInt(costPixels)
            writeInt(costVip)
            writeInt(1)

            writeUTF(furnishing.type.type)
            writeInt(furnishing.spriteId)

            if (itemName == "wallpaper" || itemName == "floor" || itemName == "landscape") writeUTF(
                catalogName.split(
                    '_'
                )[2]
            )
            else writeUTF("")

            writeInt(amount)
            writeInt(-1) // ????
            writeBoolean(clubOnly)
        }
    }

    private fun serializeItem(
        habboResponse: HabboResponse,
        furnishing: Furnishing,
        amount: Int
    ) {
        habboResponse.apply {
            writeUTF(furnishing.type.type) // productType

            if (furnishing.type == ItemType.BADGE) {
                writeUTF(furnishing.itemName) // extraParam for badge
                writeInt(1) // count for badge
            } else {
                writeInt(furnishing.spriteId) // furniClassId
                writeUTF(
                    if (itemName == "wallpaper" || itemName == "floor" || itemName == "landscape") catalogName.split(
                        '_'
                    )[2] else ""
                ) // extraParam
                writeInt(amount) // productCount
                writeBoolean(limited) // isUnique (limited)

                if (limited) {
                    writeInt(limitedTotal) // uniqueLimitedItemSeriesSize
                    writeInt(limitedTotal - limitedSells.get()) // uniqueLimitedItemsLeft
                }
            }
        }
    }
}