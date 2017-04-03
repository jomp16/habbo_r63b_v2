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

package tk.jomp16.habbo.game.item.wired.trigger.triggers

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.wired.trigger.WiredTrigger
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

class WiredTriggerSaysSomething(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    var message = ""
    var onlyOwner = false

    init {
        if (roomItem.wiredData != null) {
            message = roomItem.wiredData.message
            onlyOwner = if (roomItem.wiredData.options.size == 1) roomItem.wiredData.options[0] == 1 else false
        }
    }

    override fun onTrigger(roomUser: RoomUser?, data: Any?): Boolean {
        if (data == null || data !is String || message.isEmpty() || roomUser == null) return false

        return data.toLowerCase().contains(message) && (onlyOwner || room.hasRights(roomUser.habboSession, true))
    }

    override fun setData(habboRequest: HabboRequest): Boolean {
        if (roomItem.wiredData == null) return false

        habboRequest.readInt() // useless?

        onlyOwner = habboRequest.readInt() == 1
        message = habboRequest.readUTF().trim().toLowerCase()

        if (message.length > 100) message = message.substring(0, 100)

        roomItem.wiredData.message = message
        roomItem.wiredData.options = listOf(if (onlyOwner) 1 else 0)

        return true
    }
}