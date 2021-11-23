/*
 * Copyright (C) 2015-2021 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.inventory

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.user.UserItem

@Suppress("unused", "UNUSED_PARAMETER")
class InventoryNewObjectsResponse {
    @Response(Outgoing.INVENTORY_NEW_OBJECTS)
    fun response(habboResponse: HabboResponse, execute: Boolean, type: Int, ids: Collection<Int>) {
        habboResponse.apply {
            if (!execute) {
                writeInt(0)

                return
            }

            writeInt(1)
            writeInt(type)
            writeInt(ids.size)

            ids.forEach { writeInt(it) }
        }
    }

    @ResponseR63A(OutgoingR63A.INVENTORY_NEW_OBJECTS)
    fun responseR63A(habboResponse: HabboResponse, userItems: Collection<UserItem>) {
        habboResponse.apply {
            writeInt(2)

            writeInt(1) // floor items
            userItems.filter { it.furnishing.type == ItemType.FLOOR }.let { floorItems ->
                writeInt(floorItems.size)

                floorItems.map { it.id }.forEach { writeInt(it) }
            }

            writeInt(2) // wall items
            userItems.filter { it.furnishing.type == ItemType.WALL }.let { wallItems ->
                writeInt(wallItems.size)

                wallItems.map { it.id }.forEach { writeInt(it) }
            }
        }
    }
}