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

package tk.jomp16.habbo.game.item.user

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.IHabboResponseSerialize
import tk.jomp16.habbo.game.item.Furnishing
import tk.jomp16.habbo.game.item.ItemType
import tk.jomp16.habbo.game.item.LimitedItemData

data class UserItem(
        val id: Int,
        var userId: Int,
        val baseItem: String,
        var extraData: String,
        val limitedId: Int,
        val limitedItemData: LimitedItemData?
                   ) : IHabboResponseSerialize {
    val furnishing: Furnishing
        get() = HabboServer.habboGame.itemManager.furnishings[baseItem]!!

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(id)
            writeUTF(furnishing.type.type.toUpperCase())
            writeInt(id)
            writeInt(furnishing.spriteId)

            HabboServer.habboGame.itemManager.writeExtradata(habboResponse, extraData, furnishing, limitedItemData)

            writeBoolean(furnishing.allowRecycle)
            writeBoolean(furnishing.allowTrade)
            writeBoolean(limitedId == 0 && furnishing.allowInventoryStack)
            writeBoolean(furnishing.allowMarketplaceSell)
            writeInt(-1) // seconds to expire rental
            writeBoolean(true)
            writeInt(-1) // room id

            if (furnishing.type == ItemType.FLOOR) {
                writeUTF("")
                writeInt(0)
            }
        }
    }
}