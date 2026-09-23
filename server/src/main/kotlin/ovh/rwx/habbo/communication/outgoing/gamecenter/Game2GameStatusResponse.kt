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

import ovh.rwx.habbo.game.snowwar.ISnowWarGameEvent

@Suppress("unused", "UNUSED_PARAMETER")
class Game2GameStatusResponse {
    @Response(Outgoing.GAME_2_GAME_STATUS)
    fun response(habboResponse: HabboResponse, data: Game2GameStatusData) {
        habboResponse.apply {
            writeInt(data.turn)
            writeInt(data.checksum)
            writeInt(data.subTurns.size)
            for (subTurnEvents in data.subTurns) {
                writeInt(subTurnEvents.size)
                for (event in subTurnEvents) {
                    writeInt(event.eventTypeId)
                    event.serialize(this)
                }
            }
        }
    }
}

data class Game2GameStatusData(
    val turn: Int = 0,
    val checksum: Int = 0,
    val subTurns: List<List<ISnowWarGameEvent>> = emptyList()
)

