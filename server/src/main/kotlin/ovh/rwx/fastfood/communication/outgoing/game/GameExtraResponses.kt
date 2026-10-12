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

package ovh.rwx.fastfood.communication.outgoing.game

import ovh.rwx.fastfood.communication.FFResponse
import ovh.rwx.fastfood.communication.outgoing.FFOutgoing
import ovh.rwx.fastfood.game.PowerUpEventData
import ovh.rwx.habbo.communication.HabboResponse

@Suppress("unused", "UNUSED_PARAMETER")
class GameExtraResponses {
    @FFResponse(FFOutgoing.POWER_UP_GAINED)
    fun powerUpGained(habboResponse: HabboResponse, data: PowerUpEventData) {
        habboResponse.apply {
            writeInt(data.count) // wire: count ANTES do type (parser §_-9Y§)
            writeInt(data.type)
        }
    }

    @FFResponse(FFOutgoing.GLOBAL_SCORES)
    fun globalScores(habboResponse: HabboResponse, scores: List<Pair<String, Int>>) {
        habboResponse.apply {
            writeInt(scores.size)
            scores.forEach { (name, score) ->
                writeUTF(name)
                writeInt(score)
            }
        }
    }

    @FFResponse(FFOutgoing.FAST_FOOD_MAINENTANCE)
    fun maintenance(habboResponse: HabboResponse, inMaintenance: Boolean, showButton: Boolean) {
        habboResponse.apply {
            writeBoolean(inMaintenance)
            writeBoolean(showButton)
        }
    }
}
