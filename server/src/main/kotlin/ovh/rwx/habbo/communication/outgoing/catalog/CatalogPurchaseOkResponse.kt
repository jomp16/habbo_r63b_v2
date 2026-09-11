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

package ovh.rwx.habbo.communication.outgoing.catalog

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.catalog.CatalogItem
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.user.UserItem

/**
 * Payload estruturado para confirmação de compra de catálogo (CATALOG_PURCHASE_OK).
 */
data class CatalogPurchaseOkData(
    val itemId: Int = 0,
    val itemName: String = "",
    val costCredits: Int = 0,
    val costPixels: Int = 0,
    val activityPointType: Int = 0,
    val clubLevel: Int = 0,
    val isRentable: Boolean = false,
    val isGift: Boolean = false,
    val userItems: Collection<UserItem> = emptyList(),
) {
    constructor(catalogItem: CatalogItem, userItems: Collection<UserItem> = emptyList()) : this(
        itemId = catalogItem.id,
        itemName = catalogItem.catalogName.ifEmpty { catalogItem.furnishing.itemName },
        costCredits = catalogItem.costCredits,
        costPixels = catalogItem.costPixels,
        activityPointType = if (catalogItem.costPixels > 0) 0 else 5,
        clubLevel = if (catalogItem.clubOnly) 1 else 0,
        userItems = userItems,
    )
}

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogPurchaseOkResponse {
    @Response(Outgoing.CATALOG_PURCHASE_OK)
    @ResponseR63A(OutgoingR63A.CATALOG_PURCHASE_OK)
    fun response(habboResponse: HabboResponse, data: CatalogPurchaseOkData) {
        habboResponse.apply {
            writeInt(data.itemId)
            writeUTF(data.itemName)
            if (isVersionAtLeast(2011, 11, 10)) {
                writeBoolean(data.isRentable)
            }
            writeInt(data.costCredits)
            writeInt(data.costPixels)
            writeInt(data.activityPointType)
            if (isVersionAtLeast(2011, 11, 10)) {
                writeBoolean(data.isGift)
            }
            writeInt(data.userItems.size)

            data.userItems.forEachIndexed { i, userItem ->
                writeUTF(userItem.furnishing.type.type)

                when (userItem.furnishing.type) {
                    ItemType.BADGE -> {
                        writeUTF(userItem.furnishing.itemName)
                    }

                    else -> {
                        writeInt(userItem.furnishing.spriteId)
                        writeUTF(userItem.furnishing.itemName)
                        writeInt(i + 1)
                        if (isVersionAtLeast(2011, 11, 10)) {
                            writeBoolean(userItem.limitedItemData != null)

                            userItem.limitedItemData?.let {
                                writeInt(it.limitedNumber)
                                writeInt(it.limitedTotal)
                            }
                        } else {
                            writeInt(0) // expiration
                        }
                    }
                }
            }

            writeInt(data.clubLevel)
            if (isVersionAtLeast(2011, 11, 10)) {
                writeBoolean(true)
            }
        }
    }
}