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
 * Resultado de um pedido de ignorar/designorar.
 *
 * @param result 0 recusado, 1 adicionado, 2 adicionado e o cliente descarta o mais antigo, 3 removido.
 * @param targetUserId Id do alvo (era por id); 0 se não resolvido.
 * @param targetName Username do alvo (era por nome, a partir de 2012-04-02).
 */
data class RoomIgnoreUserResultData(val result: Int, val targetUserId: Int, val targetName: String)

@Suppress("unused", "UNUSED_PARAMETER")
class RoomIgnoreUserResultResponse {
    @Response(Outgoing.ROOM_IGNORE_USER_RESULT)
    @ResponseR63A(OutgoingR63A.ROOM_IGNORE_USER_RESULT)
    fun response(habboResponse: HabboResponse, data: RoomIgnoreUserResultData) {
        habboResponse.apply {
            writeInt(data.result)

            if (isVersionAtLeast(2025, 12, 10)) {
                writeInt(data.targetUserId)
            } else if (isVersionAtLeast(2012, 4, 2)) {
                writeUTF(data.targetName)
            }
        }
    }
}
