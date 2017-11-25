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

package ovh.rwx.habbo.game.item.interactors

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.item.ItemInteractor
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.util.Vector3

class RollerFurniInteractor : ItemInteractor() {
    override fun onCycle(room: Room, roomItem: RoomItem) {
        super.onCycle(room, roomItem)
        val frontVector2 = roomItem.getFrontPosition()
        val frontVector3 = Vector3(frontVector2, room.roomGamemap.getAbsoluteHeight(frontVector2))
        var reCycle = true

        if (!room.roomGamemap.isBlocked(frontVector2)) {
            // moving players
            room.roomGamemap.roomUserMap[roomItem.position.vector2]?.filter { !it.walking }?.forEach {
                val copy = it.currentVector3

                if (it.moveTo(frontVector2, rollerId = roomItem.id)) {
                    room.sendHabboResponse(Outgoing.ROOM_ROLLER, copy, frontVector3, it.virtualID, roomItem.id, -1)

                    reCycle = false
                }
            }
            // Moving items
            room.roomGamemap.roomItemMap[roomItem.position.vector2]?.filter { it.id != roomItem.id && it.position.z > roomItem.position.z }?.let {
                val roomItems = if (it.size > 10) it.take(10) else it

                roomItems.forEach {
                    if (room.setFloorItem(it, frontVector2, it.rotation, null, rollerId = roomItem.id)) reCycle = false
                }
            }
        }

        if (reCycle) roomItem.requestCycles(HabboServer.habboConfig.timerConfig.roller)
    }
}