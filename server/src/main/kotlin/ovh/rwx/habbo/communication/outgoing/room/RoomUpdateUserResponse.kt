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
 * Payload estruturado para atualização de usuário no quarto (USER_UPDATE).
 *
 * @property virtualId ID virtual do usuário.
 * @property figure Figura (look) do usuário.
 * @property gender Gênero do usuário.
 * @property motto Motto do usuário.
 * @property achievementScore Pontuação de conquistas.
 */
data class RoomUpdateUserData(
    val virtualId: Int,
    val figure: String,
    val gender: String,
    val motto: String,
    val achievementScore: Int,
)

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUpdateUserResponse {
    @Response(Outgoing.USER_UPDATE)
    @ResponseR63A(OutgoingR63A.USER_UPDATE)
    fun response(
        habboResponse: HabboResponse,
        data: RoomUpdateUserData
    ) {
        habboResponse.apply {
            writeInt(data.virtualId)
            writeUTF(data.figure)
            writeUTF(data.gender)
            writeUTF(data.motto)
            writeInt(data.achievementScore)

            if (isVersionAtLeast(2026, 8, 6)) {
                writeUTF("") // 1. string ignorada (provável swim figure) - TODO
                val items = emptyList<Triple<Int, Int, Int>>() // 2. TODO: usar lista real
                writeInt(items.size)
                items.forEach { (a, b, c) -> // 3. 3 ints por item - ignorados pelo cliente
                    writeInt(a)
                    writeInt(b)
                    writeInt(c)
                }
                writeInt(0) // 4. badgesRank - TODO
            }
        }
    }
}