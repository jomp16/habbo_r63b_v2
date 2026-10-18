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

package ovh.rwx.habbo.game.room.user

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.util.Vector3

class RoomBot(
    val botId: Int,
    val botName: String,
    val motto: String,
    val figure: String,
    val gender: String,
    room: Room,
    virtualID: Int,
    currentVector3: Vector3,
    headRotation: Int,
    bodyRotation: Int
) : RoomHumanoid(room, virtualID, currentVector3, headRotation, bodyRotation) {

    /**
     * Serialização unificada — o ramo bot (userType == 3) do UsersMessageParser NÃO lê
     * nada além dos campos comuns em nenhuma era (verificado de 2011-05-06 a WIN63-2026:
     * o ramo só faz fallback de figure e sex="M"). Os campos ownerId/ownerName/"5" dos
     * gêmeos antigos eram corrupção de wire que desyncava o próximo item do loop.
     */
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(botId * -1)
            writeUTF(botName)
            writeUTF(motto)
            writeUTF(figure)
            writeInt(virtualID)
            writeInt(currentVector3.x)
            writeInt(currentVector3.y)
            writeUTF(currentVector3.z.toString())
            writeInt(bodyRotation)
            writeInt(3) // 1 for user, 2 for pet, 3 for bot.
        }
    }
}
