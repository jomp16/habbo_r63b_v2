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

package ovh.rwx.habbo.game.item.wired.trigger.triggers

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

class WiredTriggerWalksOffFurni(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    private val roomItemsIds: MutableList<Int> = mutableListOf()

    init {
        roomItem.wiredData?.let {
            roomItemsIds.addAll(it.items)
        }
    }

    override fun onTrigger(roomUser: RoomUser?, data: Any?): Boolean = data != null && data is RoomItem && roomItemsIds.any { it == data.id }

    override fun setData(habboRequest: HabboRequest): Boolean {
        roomItem.wiredData?.let {
            roomItemsIds.clear()

            habboRequest.readInt() // useless?
            habboRequest.readUTF() // useless?
            val amount = habboRequest.readInt()

            repeat(amount) {
                val itemId = habboRequest.readInt()

                if (room.roomItems.containsKey(itemId)) {
                    val roomItem1 = room.roomItems[itemId] ?: return@repeat

                    if (!roomItem1.furnishing.interactionType.name.startsWith("WIRED")) roomItemsIds += itemId
                }
            }

            it.items = roomItemsIds.toList()

            return true
        }

        return false
    }
}