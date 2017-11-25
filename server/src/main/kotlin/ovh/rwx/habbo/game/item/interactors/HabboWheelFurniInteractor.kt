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
import ovh.rwx.habbo.util.Utils

class HabboWheelFurniInteractor : ItemInteractor() {
    override fun onPlace(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        super.onPlace(room, roomUser, roomItem)

        roomItem.extraData = "1"
    }

    override fun onRemove(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        super.onRemove(room, roomUser, roomItem)

        roomItem.extraData = "-1"
    }

    override fun onTrigger(room: Room, roomUser: RoomUser?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomUser, roomItem, hasRights, request)

        if (!hasRights) return

        if (roomItem.extraData != "-1") {
            roomItem.extraData = "-1"
            roomItem.update(false, true)
            roomItem.requestCycles(10)
        }

        if (roomUser != null) room.wiredHandler.triggerWired(WiredTriggerStateChanged::class, roomUser, roomItem)
    }

    override fun onCycle(room: Room, roomItem: RoomItem) {
        super.onCycle(room, roomItem)

        roomItem.extraData = Utils.randInt(1 until 10).toString()

        roomItem.update(true, true)
    }
}