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

package tk.jomp16.habbo.communication.incoming.room

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.clothing.ClothingDao
import tk.jomp16.habbo.database.item.ItemDao
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomItemUseClothingHandler {
    @Handler(Incoming.ROOM_ITEM_USE_CLOTHING)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null || !habboSession.currentRoom!!.hasRights(habboSession)) return

        val itemId = habboRequest.readInt()
        val roomItem = habboSession.currentRoom!!.roomItems[itemId] ?: return

        if (roomItem.userId != habboSession.userInformation.id || !roomItem.furnishing.itemName.startsWith("clothing_")) return

        habboSession.currentRoom!!.removeItem(habboSession.roomUser!!, roomItem)

        habboSession.userInformation.clothings += roomItem.itemName

        habboSession.sendHabboResponse(Outgoing.USER_CLOTHINGS, habboSession.userInformation.clothings)

        habboSession.sendSuperNotification("", "title", "\${notification.figureset.redeemed.success.title}", "message", "\${notification.figureset.redeemed.success.message}", "linkUrl", "event:avatareditor/open", "linkTitle", "\${notification.figureset.redeemed.success.linkTitle}")

        ClothingDao.addClothing(habboSession.userInformation.id, roomItem.itemName)
        ItemDao.deleteItem(itemId)
    }
}