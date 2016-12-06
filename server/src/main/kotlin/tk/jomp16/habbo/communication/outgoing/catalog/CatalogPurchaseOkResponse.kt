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

package tk.jomp16.habbo.communication.outgoing.catalog

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.catalog.CatalogItem
import tk.jomp16.habbo.game.item.ItemType
import tk.jomp16.habbo.game.item.user.UserItem

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogPurchaseOkResponse {
    @Response(Outgoing.CATALOG_PURCHASE_OK)
    fun response(habboResponse: HabboResponse, catalogItem: CatalogItem, userItems: Collection<UserItem>) {
        habboResponse.apply {
            writeInt(catalogItem.id)
            writeUTF(if (catalogItem.catalogName.isEmpty()) catalogItem.furnishing!!.itemName else catalogItem.catalogName)
            writeBoolean(false) // is rentable
            writeInt(catalogItem.costCredits)
            writeInt(catalogItem.costPixels)
            writeInt(if (catalogItem.costPixels > 0) 0 else 5) // activityPointType
            writeBoolean(false) // is gift
            writeInt(userItems.size)

            userItems.forEachIndexed { i, userItem ->
                writeUTF(userItem.furnishing.type.type)

                when (userItem.furnishing.type) {
                    ItemType.BADGE -> {
                        writeUTF(userItem.furnishing.itemName)
                    }
                    else -> {
                        writeInt(userItem.furnishing.spriteId)
                        writeUTF(userItem.furnishing.itemName)
                        writeInt(i + 1)
                        writeBoolean(userItem.limitedItemData != null)

                        userItem.limitedItemData?.let {
                            writeInt(it.limitedNumber)
                            writeInt(it.limitedTotal)
                        }
                    }
                }
            }

            writeInt(if (catalogItem.clubOnly) 1 else 0) // club level, probably
            writeBoolean(true)
        }
    }
}