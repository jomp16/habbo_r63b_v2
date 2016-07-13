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

package tk.jomp16.habbo.communication.incoming.room

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.game.item.ItemType
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.util.Vector2

@Suppress("unused", "UNUSED_PARAMETER")
class RoomPlaceItemHandler {
    @Handler(Incoming.ROOM_PLACE_ITEM)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null) return

        // floor = [0][7]3 8 4 2
        // wall  = [0][19]2 :w=2,11 l=11,36 l

        if (!habboSession.currentRoom?.hasRights(habboSession)!!) {
            habboSession.sendSuperNotification("furni_placement_error", "message", "\${room.error.cant_set_not_owner}")

            return
        }

        val rawDataSplit = habboRequest.readUTF().split(' ')
        val itemId = rawDataSplit[0].toInt()
        val userItem = habboSession.habboInventory.items[itemId] ?: return

        // todo: remove item from inventory plox

        if (userItem.furnishing.type == ItemType.WALL) {
            // parse wall data
            val correctedWallData = rawDataSplit.drop(1)

            if (correctedWallData.size < 3) return

            val roomItem = HabboServer.habboGame.itemManager.getRoomItemFromUserItem(
                    habboSession.currentRoom!!.roomData.id, userItem)

            println(roomItem)

            // todo: place wall item
        } else {
            // parse floor data
            if (rawDataSplit.size < 4) return

            val x = rawDataSplit[1].toInt()
            val y = rawDataSplit[2].toInt()
            val rot = rawDataSplit[3].toInt()

            val roomItem = HabboServer.habboGame.itemManager.getRoomItemFromUserItem(
                    habboSession.currentRoom!!.roomData.id, userItem)

            if (habboSession.currentRoom!!.setFloorItem(roomItem, Vector2(x, y), rot,
                                                        habboSession.userInformation.username)) {
                habboSession.habboInventory.removeItem(userItem)
            }
        }
    }
}