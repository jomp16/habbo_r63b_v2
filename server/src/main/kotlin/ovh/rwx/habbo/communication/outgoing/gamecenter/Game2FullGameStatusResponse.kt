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

package ovh.rwx.habbo.communication.outgoing.gamecenter

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.snowwar.objects.SnowWarGameObject

@Suppress("unused", "UNUSED_PARAMETER")
class Game2FullGameStatusResponse {
    @Response(Outgoing.GAME_2_FULL_GAME_STATUS)
    fun response(
        habboResponse: HabboResponse,
        data: Game2FullGameStatusData
    ) {
        habboResponse.apply {
            writeInt(0)
            writeInt(data.remainingTimeSeconds)
            writeInt(data.durationInSeconds)

            // GameObjectsData
            writeInt(data.objects.size)
            data.objects.forEach { obj ->
                obj.serialize(this)
            }

            writeInt(0)
            writeInt(data.numberOfTeams)

            // GameStatusData
            writeInt(data.status.turn)
            writeInt(data.status.checksum)
            writeInt(data.status.subTurns.size)
            for (subTurnEvents in data.status.subTurns) {
                writeInt(subTurnEvents.size)
                for (event in subTurnEvents) {
                    writeInt(event.eventTypeId)
                    event.serialize(this)
                }
            }
        }
    }
}

data class Game2FullGameStatusData(
    val remainingTimeSeconds: Int = 120,
    val durationInSeconds: Int = 120,
    val objects: Collection<SnowWarGameObject> = emptyList(),
    val numberOfTeams: Int = 2,
    val status: Game2GameStatusData = Game2GameStatusData()
)
