/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.item.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.LimitedItemData
import java.io.Serializable

data class UserItem(
        val id: Int,
        var userId: Int,
        val itemName: String,
        var extraData: String,
        val limited: Boolean
) : IHabboResponseSerialize, Serializable {
    val limitedItemData: LimitedItemData? by lazy { if (limited) ItemDao.getLimitedData(id) else null }
    val furnishing: Furnishing
        get() = HabboServer.habboGame.itemManager.furnishings[itemName]!!

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(id)
            writeUTF(furnishing.type.type.toUpperCase())
            writeInt(id)
            writeInt(furnishing.spriteId)

            HabboServer.habboGame.itemManager.writeExtradata(habboResponse, extraData, furnishing, limitedItemData)

            writeBoolean(furnishing.allowRecycle)
            writeBoolean(furnishing.allowTrade)
            writeBoolean(limitedItemData == null && furnishing.allowInventoryStack)
            writeBoolean(furnishing.allowMarketplaceSell)
            writeInt(-1) // milliseconds to expire rental
            writeBoolean(true)
            writeInt(-1) // room id
            if (furnishing.type == ItemType.FLOOR) {
                writeUTF("")
                writeInt(0)
            }
        }
    }
}