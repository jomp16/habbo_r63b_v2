/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.game.user.inventory

import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.item.ItemDao
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.user.UserItem
import tk.jomp16.habbo.game.user.HabboSession
import java.util.*

class HabboInventory(private val habboSession: HabboSession) {
    val items: MutableMap<Int, UserItem> = HashMap()
    val roomItemsToRemove: MutableList<RoomItem> by lazy { ArrayList<RoomItem>() }

    init {
        items += ItemDao.getUserItems(habboSession.userInformation.id).associateBy { it.id }
    }

    fun addItems(userItems: List<UserItem>) {
        items += userItems.associateBy { it.id }

        habboSession.sendHabboResponse(Outgoing.INVENTORY_NEW_OBJECTS, true, 1, userItems.map { it.id })
        habboSession.sendHabboResponse(Outgoing.INVENTORY_UPDATE)
    }

    fun removeItem(userItem: UserItem, delete: Boolean = false) {
        if (!items.containsKey(userItem.id)) return

        items.remove(userItem.id)

        habboSession.sendHabboResponse(Outgoing.INVENTORY_REMOVE_OBJECT, userItem.id)

        if (delete) ItemDao.deleteItem(userItem.id)
    }
}