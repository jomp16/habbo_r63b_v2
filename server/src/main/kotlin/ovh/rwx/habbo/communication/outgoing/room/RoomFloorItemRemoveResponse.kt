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
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.room.RoomItem

/**
 * Payload estruturado para remoção de item de chão do quarto (ROOM_FLOOR_ITEM_REMOVE).
 *
 * @param roomItem Item removido.
 * @param expired Indica se o item expirou (ex: aluguel).
 * @param delay Atraso na remoção visual (R63B+).
 */
data class RoomFloorItemRemoveData(
    val roomItem: RoomItem,
    val expired: Boolean = false,
    val delay: Int = 0,
)

@Suppress("unused", "UNUSED_PARAMETER")
class RoomFloorItemRemoveResponse {
    @Response(Outgoing.ROOM_FLOOR_ITEM_REMOVE)
    @ResponseR63A(OutgoingR63A.ROOM_FLOOR_ITEM_REMOVE)
    fun response(habboResponse: HabboResponse, data: RoomFloorItemRemoveData) {
        habboResponse.apply {
            writeUTF(data.roomItem.id.toString())

            // A partir de 26/03/2013 (RELEASE63-201303261200), expired passou a ser 1 byte (Boolean).
            // Anteriormente (R63A e R63B inicial), era serializado como Int (4 bytes, 1 ou 0).
            if (isVersionAtLeast(2013, 3, 26)) {
                writeBoolean(data.expired)
            } else {
                writeInt(if (data.expired) 1 else 0)
            }

            // A partir de 20/10/2011 (RELEASE63-201110200821), pickerId (userId) foi introduzido.
            if (isVersionAtLeast(2011, 10, 20)) {
                writeInt(data.roomItem.userId)
            }

            // A partir de 26/03/2013 (RELEASE63-201303261200), delay foi introduzido no 4º campo.
            if (isVersionAtLeast(2013, 3, 26)) {
                writeInt(data.delay)
            }
        }
    }
}