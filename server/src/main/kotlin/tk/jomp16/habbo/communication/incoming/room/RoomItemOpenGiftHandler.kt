/*
 * Copyright (C) 2015-2017 jomp16
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

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.item.ItemDao
import tk.jomp16.habbo.game.item.Furnishing
import tk.jomp16.habbo.game.item.InteractionType
import tk.jomp16.habbo.game.item.ItemType
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.util.Vector3
import java.util.concurrent.TimeUnit

@Suppress("unused", "UNUSED_PARAMETER")
class RoomItemOpenGiftHandler {
    @Handler(Incoming.ROOM_ITEM_OPEN_GIFT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null) return
        val giftItemId = habboRequest.readInt()
        val giftRoomItem = habboSession.currentRoom!!.roomItems[giftItemId]

        if (giftRoomItem == null || giftRoomItem.userId != habboSession.userInformation.id) return

        if (giftRoomItem.furnishing.itemName == HabboServer.habboConfig.recyclerConfig.giftBox) {
            // recycler box, doesn't need magic remove, open box right now
            openBox(habboSession, giftRoomItem)
        } else {
            // normal gift, do a magic remove and then open box
            giftRoomItem.magicRemove = true
            giftRoomItem.update(false, true)

            HabboServer.serverScheduledExecutor.schedule({ openBox(habboSession, giftRoomItem) }, 3, TimeUnit.SECONDS)
        }
    }

    private fun openBox(habboSession: HabboSession, giftRoomItem: RoomItem) {
        val giftData = ItemDao.getGiftData(giftRoomItem.id) ?: return
        // replace current gift item to the gifted item
        habboSession.currentRoom!!.removeItem(habboSession.roomUser, giftRoomItem)
        val shouldAddToRoom = giftData.furnishing.type == ItemType.FLOOR && giftData.amount == 1

        if (shouldAddToRoom) {
            val roomItem = RoomItem(giftRoomItem.id, giftRoomItem.userId, giftRoomItem.roomId, giftData.itemName, giftData.extradata, Vector3(0, 0, 0.toDouble()), 0, "")

            habboSession.currentRoom!!.setFloorItem(roomItem, giftRoomItem.position.vector2, giftRoomItem.rotation, habboSession.roomUser)
        } else if (giftData.furnishing.interactionType == InteractionType.TELEPORT) {
            // todo: add custom item types here, like teleports, dimmers, etc
            // either gift isn't a floor item, or amount > 1
            val furnishings = mutableListOf<Furnishing>()
            val extraDatas = mutableListOf<String>()

            repeat(giftData.amount) {
                furnishings += giftData.furnishing
                extraDatas += giftData.extradata
            }

            habboSession.habboInventory.addItems(ItemDao.addItems(habboSession.userInformation.id, furnishings, extraDatas))
        }

        habboSession.sendHabboResponse(Outgoing.ROOM_ITEM_OPEN_GIFT_RESULT, giftRoomItem.id, giftRoomItem.extraData, giftRoomItem.furnishing, shouldAddToRoom)

        ItemDao.deleteItems(listOf(giftRoomItem.id)) // remove gift item forever
        ItemDao.deleteGiftData(giftData.id)
    }
}