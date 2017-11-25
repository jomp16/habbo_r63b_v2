/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.Vector2

@Suppress("unused", "UNUSED_PARAMETER")
class RoomPlaceItemHandler {
    @Handler(Incoming.ROOM_PLACE_ITEM, Incoming.ROOM_PLACE_POST_IT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null) return
        // floor = [0][7]3 8 4 2
        // wall  = [0][19]2 :w=2,11 l=11,36 l
        // postit = [0][0][0]2[0][16]:w=4,7 l=11,11 l
        if (!habboSession.currentRoom?.hasRights(habboSession)!!) {
            habboSession.sendSuperNotification("furni_placement_error", "message", "\${room.error.cant_set_not_owner}")

            return
        }
        val rawDataSplit: List<String>
        val itemId: Int

        if (habboRequest.incoming == Incoming.ROOM_PLACE_POST_IT) {
            itemId = habboRequest.readInt()
            val extraData = habboRequest.readUTF().split(' ')

            rawDataSplit = listOf("", extraData[0], extraData[1], extraData[2])
        } else {
            rawDataSplit = habboRequest.readUTF().split(' ')
            itemId = rawDataSplit[0].toInt()
        }
        val userItem = habboSession.habboInventory.items[itemId] ?: return
        val success: Boolean

        if (userItem.furnishing.type == ItemType.WALL) {
            // parse wall data
            val correctedWallData = rawDataSplit.drop(1)

            if (correctedWallData.size < 3) return
            val roomItem = HabboServer.habboGame.itemManager.getRoomItemFromUserItem(habboSession.currentRoom!!.roomData.id,
                    userItem)

            success = habboSession.currentRoom!!.setWallItem(roomItem, correctedWallData, habboSession.roomUser)
        } else {
            // parse floor data
            if (rawDataSplit.size < 4) return
            val x = rawDataSplit[1].toInt()
            val y = rawDataSplit[2].toInt()
            val rot = rawDataSplit[3].toInt()
            val roomItem = HabboServer.habboGame.itemManager.getRoomItemFromUserItem(habboSession.currentRoom!!.roomData.id, userItem)

            success = habboSession.currentRoom!!.setFloorItem(roomItem, Vector2(x, y), rot, habboSession.roomUser)
        }

        if (success) habboSession.habboInventory.removeItems(listOf(itemId))
    }
}