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
import ovh.rwx.habbo.communication.outgoing.misc.MiscSuperNotificationResponse
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.Vector2

@Suppress("unused", "UNUSED_PARAMETER")
class RoomItemMoveFloorHandler {
    @Handler(Incoming.ROOM_MOVE_FLOOR_ITEM)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null) return
        val itemId = habboRequest.readInt()
        val x = habboRequest.readInt()
        val y = habboRequest.readInt()
        val rotation = habboRequest.readInt()
        val newPosition = Vector2(x, y)

        if (!habboSession.currentRoom!!.roomItems.containsKey(itemId)) return

        val roomItem = habboSession.currentRoom!!.roomItems[itemId]!!

        if (habboSession.currentRoom?.setFloorItem(roomItem, newPosition, rotation, habboSession.roomUser) == false) {
            if (roomItem.position.vector2 != newPosition) {
                habboSession.sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.FURNITURE_PLACEMENT_ERROR, "message", "\${room.error.cant_set_item}")
            }

            roomItem.update(updateDb = false, updateClient = true)
        }
    }
}