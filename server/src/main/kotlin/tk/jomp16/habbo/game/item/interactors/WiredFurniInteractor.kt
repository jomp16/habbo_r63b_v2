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

import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.item.ItemInteractor
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

class WiredFurniInteractor : ItemInteractor() {
    override fun onPlace(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        super.onPlace(room, roomUser, roomItem)

        roomItem.extraData = "0"
    }

    override fun onRemove(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        super.onRemove(room, roomUser, roomItem)

        roomItem.extraData = "0"
    }

    override fun onTrigger(room: Room, roomUser: RoomUser?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomUser, roomItem, hasRights, request)

        if (!hasRights || roomItem.wiredData == null) return

        roomItem.extraData = "1"
        roomItem.update(false, true)
        roomItem.requestCycles(1)

        val outgoing =
                if (roomItem.furnishing.interactionType.name.startsWith("WIRED_TRIGGER")) Outgoing.ROOM_WIRED_TRIGGER_DIALOG
                else if (roomItem.furnishing.interactionType.name.startsWith("WIRED_ACTION")) Outgoing.ROOM_WIRED_EFFECT_DIALOG
                else if (roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION")) Outgoing.ROOM_WIRED_EFFECT_DIALOG
                else return

        roomUser?.habboSession?.sendHabboResponse(outgoing, roomItem, roomItem.wiredData)
    }

    override fun onCycle(room: Room, roomItem: RoomItem) {
        super.onCycle(room, roomItem)

        if (roomItem.extraData == "1") {
            roomItem.extraData = "0"

            roomItem.update(false, true)
        }
    }
}