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
 * Payload estruturado para itens de chão do quarto (ROOM_FLOOR_ITEMS).
 *
 * @param ownerId ID do proprietário do quarto/grupo.
 * @param ownerName Nome do proprietário do quarto/grupo.
 * @param floorItems Coleção de itens de chão presentes no quarto.
 */
data class RoomFloorItemsData(
    val ownerId: Int,
    val ownerName: String,
    val floorItems: Collection<RoomItem>,
) {
    constructor(room: Room, floorItems: Collection<RoomItem>) : this(
        ownerId = room.roomData.ownerId,
        ownerName = room.roomData.ownerName,
        floorItems = floorItems,
    )
}

@Suppress("unused", "UNUSED_PARAMETER")
class RoomFloorItemsResponse {
    @Response(Outgoing.ROOM_FLOOR_ITEMS)
    @ResponseR63A(OutgoingR63A.ROOM_FLOOR_ITEMS)
    fun response(habboResponse: HabboResponse, data: RoomFloorItemsData) {
        habboResponse.apply {
            if (outgoingR63A == null) {
                // todo: group
                writeInt(1)
                writeInt(data.ownerId)
                writeUTF(data.ownerName)
            }
            writeInt(data.floorItems.size) // size
            data.floorItems.forEach { serialize(it) }
        }
    }
}