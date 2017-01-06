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

package tk.jomp16.habbo.game.item.interactors

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.item.ItemInteractor
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser
import tk.jomp16.habbo.util.Vector3

class RollerFurniInteractor : ItemInteractor() {
    override fun onCycle(room: Room, roomItem: RoomItem) {
        super.onCycle(room, roomItem)

        val frontVector2 = roomItem.getFrontPosition()
        val frontVector3 = Vector3(frontVector2, room.roomGamemap.getAbsoluteHeight(frontVector2))

        var reCycle = true

        if (!room.roomGamemap.isBlocked(frontVector2)) {
            // moving players
            room.roomGamemap.roomUserMap[roomItem.position.vector2]?.filter { !it.walking }?.forEach {
                if (it.moveTo(frontVector2, rollerId = roomItem.id)) {
                    room.sendHabboResponse(Outgoing.ROOM_ROLLER, it.currentVector3, frontVector3, it.virtualID, roomItem.id, -1)

                    reCycle = false
                }
            }

            // Moving items
            room.roomGamemap.roomItemMap[roomItem.position.vector2]?.filter { it.id != roomItem.id && it.position.z > roomItem.position.z }?.let {
                val roomItems = if (it.size > 10) it.take(10) else it

                roomItems.forEach {
                    /*var x = frontVector2.x
                    var y = frontVector2.y

                    if (roomItem.rotation == 0 || roomItem.rotation == 4) {
                        if (it.rotation == 2 || it.rotation == 6) {
                            if (it.position.y != roomItem.position.y && roomItem.rotation == 0) y -= (it.furnishing.width - 1)
                            if (it.position.x != roomItem.position.x) x -= (it.furnishing.height - 1)
                        } else {
                            if (it.position.y != roomItem.position.y && roomItem.rotation == 0) y -= (it.furnishing.height - 1)
                            if (it.position.x != roomItem.position.x) x -= (it.furnishing.width - 1)
                        }
                    } else if (roomItem.rotation == 2 || roomItem.rotation == 6) {
                        if (it.rotation == 0 || it.rotation == 4) {
                            if (it.position.x != roomItem.position.x && roomItem.rotation == 6) x -= (it.furnishing.height - 1)
                            if (it.position.y != roomItem.position.y) y -= (it.furnishing.width - 1)
                        } else {
                            if (it.position.x != roomItem.position.x && roomItem.rotation == 6) x -= (it.furnishing.width - 1)
                            if (it.position.y != roomItem.position.y) y -= (it.furnishing.height - 1)
                        }
                    }*/

                    if (room.setFloorItem(it, frontVector2, it.rotation, null, rollerId = roomItem.id)) reCycle = false
                }
            }
        }

        if (reCycle) roomItem.requestCycles(HabboServer.habboConfig.timerConfig.roller)
    }

    override fun onUserWalksOn(room: Room, roomUser: RoomUser, roomItem: RoomItem) {
        super.onUserWalksOn(room, roomUser, roomItem)

        roomItem.requestCycles(HabboServer.habboConfig.timerConfig.roller)
    }

    override fun onUserWalksOff(room: Room, roomUser: RoomUser, roomItem: RoomItem) {
        super.onUserWalksOff(room, roomUser, roomItem)

        roomItem.requestCycles(0)
    }
}