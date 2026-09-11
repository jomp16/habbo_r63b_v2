/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.room

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room

/**
 * Payload estruturado para itens de parede do quarto (ROOM_WALL_ITEMS).
 *
 * @param ownerId ID do proprietário do quarto/grupo.
 * @param ownerName Nome do proprietário do quarto/grupo.
 * @param wallItems Coleção de itens de parede presentes no quarto.
 */
data class RoomWallItemsData(
    val ownerId: Int,
    val ownerName: String,
    val wallItems: Collection<RoomItem>,
) {
    constructor(room: Room, wallItems: Collection<RoomItem>) : this(
        ownerId = room.roomData.ownerId,
        ownerName = room.roomData.ownerName,
        wallItems = wallItems,
    )
}

@Suppress("unused", "UNUSED_PARAMETER")
class RoomWallItemsResponse {
    @Response(Outgoing.ROOM_WALL_ITEMS)
    @ResponseR63A(OutgoingR63A.ROOM_WALL_ITEMS)
    fun response(habboResponse: HabboResponse, data: RoomWallItemsData) {
        habboResponse.apply {
            if (outgoingR63A == null) {
                // todo: group
                writeInt(1)
                writeInt(data.ownerId)
                writeUTF(data.ownerName)
            }
            writeInt(data.wallItems.size) // size
            data.wallItems.forEach { serialize(it) }
        }
    }
}