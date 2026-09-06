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

/**
 * Payload estruturado para recusa de campainha do quarto (ROOM_DOORBELL_DENIED).
 *
 * @param username Nome do usuário que teve a entrada recusada (ou vazio).
 * @param roomId ID do quarto (introduzido no Flash em 23/05/2018 - PRODUCTION-201805231227-590553814).
 */
data class RoomDoorbellDeniedData(
    val username: String = "",
    val roomId: Int = 0,
)

@Suppress("unused", "UNUSED_PARAMETER")
class RoomDoorbellDeniedResponse {
    @Response(Outgoing.ROOM_DOORBELL_DENIED)
    @ResponseR63A(OutgoingR63A.ROOM_DOORBELL_DENIED)
    fun response(habboResponse: HabboResponse, data: RoomDoorbellDeniedData = RoomDoorbellDeniedData()) {
        habboResponse.apply {
            if (isVersionAtLeast(2018, 5, 23)) {
                writeInt(data.roomId)
            }
            writeUTF(data.username)
        }
    }
}