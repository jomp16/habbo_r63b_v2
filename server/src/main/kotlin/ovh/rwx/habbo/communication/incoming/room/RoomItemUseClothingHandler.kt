/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.incoming.room

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.database.clothing.ClothingDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomItemUseClothingHandler {
    @Handler(Incoming.ROOM_ITEM_USE_CLOTHING)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null || !habboSession.currentRoom!!.hasRights(habboSession)) return
        val itemId = habboRequest.readInt()
        val roomItem = habboSession.currentRoom!!.roomItems[itemId] ?: return

        if (roomItem.userId != habboSession.userInformation.id || !roomItem.furnishing.itemName.startsWith("clothing_")) return

        if (habboSession.userInformation.clothings.contains(roomItem.itemName)) {
            habboSession.sendSuperNotification("",
                    "title",
                    "\${notification.figureset.already.redeemed.title}",
                    "message",
                    "\${notification.figureset.already.redeemed.message}",
                    "linkUrl",
                    "\${notification.figureset.already.redeemed.linkUrl}",
                    "linkTitle",
                    "\${notification.figureset.already.redeemed.linkTitle}")

            return
        }

        habboSession.currentRoom!!.removeItem(habboSession.roomUser, roomItem)

        habboSession.userInformation.clothings += roomItem.itemName

        habboSession.sendHabboResponse(Outgoing.USER_CLOTHINGS, habboSession.userInformation.clothings)

        habboSession.sendSuperNotification("",
                "title",
                "\${notification.figureset.redeemed.success.title}",
                "message",
                "\${notification.figureset.redeemed.success.message}",
                "linkUrl",
                "\${notification.figureset.redeemed.success.linkUrl}",
                "linkTitle",
                "\${notification.figureset.redeemed.success.linkTitle}")

        ClothingDao.addClothing(habboSession.userInformation.id, roomItem.itemName)
        ItemDao.deleteItems(listOf(itemId))
    }
}