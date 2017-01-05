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

package tk.jomp16.habbo.game.item

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

abstract class ItemInteractor {
    open fun onPlace(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        roomItem.affectedTiles.forEach { vector2 ->
            val roomItem1 = room.roomGamemap.getHighestItem(vector2) ?: return@forEach

            if (roomItem1.furnishing.interactionType == InteractionType.PRESSURE_PAD && roomUser != null) roomItem1.onUserWalksOn(roomUser, true)
            else if (roomItem1.furnishing.interactionType == InteractionType.ROLLER) roomItem1.requestCycles(HabboServer.habboConfig.timerConfig.roller)
        }
    }

    open fun onRemove(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        roomItem.affectedTiles.forEach { vector2 ->
            val roomItem1 = room.roomGamemap.getHighestItem(vector2) ?: return@forEach

            if (roomItem1.furnishing.interactionType == InteractionType.PRESSURE_PAD) {
                roomItem1.extraData = "0"
                roomItem1.update(false, true)
            }
        }
    }

    open fun onTrigger(room: Room, roomUser: RoomUser?, roomItem: RoomItem, hasRights: Boolean, request: Int) {

    }

    open fun onCycle(room: Room, roomItem: RoomItem) {

    }

    open fun onUserWalksOn(room: Room, roomUser: RoomUser, roomItem: RoomItem) {

    }

    open fun onUserWalksOff(room: Room, roomUser: RoomUser, roomItem: RoomItem) {

    }
}