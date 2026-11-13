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
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

/**
 * Payload estruturado para informações iniciais do quarto (ROOM_INITIAL_INFO).
 *
 * @param modelName Nome/ID do modelo do quarto.
 * @param roomId ID do quarto.
 */
data class RoomInitialInfoData(
    val modelName: String,
    val roomId: Int,
)

@Suppress("unused", "UNUSED_PARAMETER")
class RoomInitialInfoResponse {
    @Response(Outgoing.ROOM_INITIAL_INFO)
    @ResponseR63A(OutgoingR63A.ROOM_INITIAL_INFO)
    fun response(habboResponse: HabboResponse, data: RoomInitialInfoData) {
        habboResponse.apply {
            // Até 24/04/2013 (RELEASE63-201304241145), RoomReadyMessageParser no Flash AS3 checava:
            // if (roomType.indexOf("model_") < 0) roomCategory = 1 (quarto público).
            // A ausência do prefixo "model_" fazia o Flash classificar o quarto privado como público,
            // disparando o alerta do Recycler ("recycler.alert.privateroom") ao trocar de sala.
            val modelStr = if (isVersionBefore(2013, 4, 24)) {
                if (data.modelName.startsWith("model_")) data.modelName else "model_${data.modelName}"
            } else {
                data.modelName
            }
            writeUTF(modelStr)
            writeInt(data.roomId)
        }
    }
}