/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.game.catalog

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.IHabboResponseSerialize
import tk.jomp16.habbo.game.item.Furnishing
import tk.jomp16.habbo.game.item.ItemType
import java.util.concurrent.atomic.AtomicInteger

data class CatalogItem(
        val id: Int,
        val pageId: Int,
        val itemName: String,
        val dealId: Int,
        val catalogName: String,
        val badge: String,
        val costCredits: Int,
        val costPixels: Int,
        val costVip: Int,
        val amount: Int,
        val clubOnly: Boolean,
        val limitedSells: AtomicInteger,
        val limitedStack: Int,
        val offerActive: Boolean
) : IHabboResponseSerialize {
    val furnishing: Furnishing?
        get() = HabboServer.habboGame.itemManager.furnishings[itemName]
    val deal: CatalogDeal?
        get() = HabboServer.habboGame.catalogManager.catalogDeals.find { it.id == dealId }

    val limited = limitedStack > 0

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(id)
            writeUTF(if (catalogName.isNotBlank() || dealId > 0) catalogName else furnishing!!.itemName)
            writeBoolean(false) // todo: is rentable

            writeInt(costCredits)

            if (costVip > 0) {
                writeInt(costVip)
                writeInt(5)
            } else {
                writeInt(costPixels)
                writeInt(0)
            }

            writeBoolean(if (dealId > 0) true else furnishing!!.canGift)

            // item count, count n item if there is no badge, otherwise count as n + 1.
            writeInt(if (dealId > 0) deal!!.furnishings.size else 1 + if (badge.isNotBlank()) 1 else 0)

            if (badge.isNotBlank()) {
                writeUTF("b")
                writeUTF(badge)
            }

            if (dealId > 0) {
                deal!!.let { deal ->
                    deal.furnishings.forEachIndexed { i, furnishing ->
                        serializeItem(habboResponse, furnishing, deal.amounts[i])
                    }
                }
            } else {
                serializeItem(habboResponse, furnishing!!, amount)
            }

            writeInt(if (clubOnly) 1 else 0)
            writeBoolean(offerActive && !limited)
            writeBoolean(true) // ?
            writeUTF("") // ?
        }
    }

    private fun serializeItem(habboResponse: HabboResponse, furnishing: Furnishing, amount: Int) {
        habboResponse.apply {
            writeUTF(furnishing.type.type)

            if (furnishing.type == ItemType.BADGE) {
                writeUTF(furnishing.itemName)
            } else {
                writeInt(furnishing.spriteId)

                if (itemName == "wallpaper" || itemName == "floor" || itemName == "landscape") writeUTF(catalogName.split('_')[2])
                else writeUTF("")

                writeInt(amount)
                writeBoolean(limited)

                if (limited) {
                    writeInt(limitedStack)
                    writeInt(limitedStack - limitedSells.get())
                }
            }
        }
    }
}