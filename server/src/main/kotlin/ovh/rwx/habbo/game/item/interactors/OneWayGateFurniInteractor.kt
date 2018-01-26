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

import ovh.rwx.habbo.game.item.ItemInteractor
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerStateChanged
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

class OneWayGateFurniInteractor : ItemInteractor() {
    override fun onPlace(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        super.onPlace(room, roomUser, roomItem)

        roomItem.extraData = "0"
        roomItem.interactingUsers.clear()
    }

    override fun onRemove(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        super.onRemove(room, roomUser, roomItem)

        roomItem.extraData = "0"
        roomItem.interactingUsers.clear()
    }

    override fun onCycle(room: Room, roomItem: RoomItem) {
        super.onCycle(room, roomItem)

        if (roomItem.interactingUsers.containsKey(1)) {
            roomItem.interactingUsers.remove(1)?.let {
                it.walkingBlocked = false

                roomItem.extraData = "0"
                roomItem.update(false, true)
            }
        }
    }

    override fun onTrigger(room: Room, roomUser: RoomUser?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomUser, roomItem, hasRights, request)

        if (roomUser == null) return

        if (!roomItem.isTouching(roomUser.currentVector3, roomUser.bodyRotation, roomItem.position.z)) {
            roomUser.moveTo(roomItem.getFrontPosition(), roomItem.getFrontRotation(), actingItem = roomItem)

            return
        }
        val behindVector2 = roomItem.getBehindPosition()

        if (room.roomGamemap.isBlocked(behindVector2)) return

        if (roomItem.interactingUsers.isEmpty()) {
            roomItem.interactingUsers[1] = roomUser
            roomItem.extraData = "1"
            roomItem.update(false, true)

            roomUser.walkingBlocked = true
            roomUser.moveTo(roomItem.getBehindPosition(), ignoreBlocking = true)

            roomItem.requestCycles(3)
        }

        room.wiredHandler.triggerWired(WiredTriggerStateChanged::class, roomUser, roomItem)
    }
}