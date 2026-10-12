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
import ovh.rwx.fastfood.game.DropFoodData
import ovh.rwx.fastfood.game.EndGameData
import ovh.rwx.fastfood.game.NextPlateData
import ovh.rwx.habbo.communication.HabboResponse

@Suppress("unused", "UNUSED_PARAMETER")
class GameActionResponses {
    @FFResponse(FFOutgoing.DROP_FOOD)
    fun dropFood(habboResponse: HabboResponse, data: DropFoodData) {
        habboResponse.apply {
            writeInt(data.seq) // wire: seq antes do playerId (parser §_-3P§)
            writeInt(data.playerId)
            writeUTF(data.locY)
            writeUTF(data.speed)
            writeInt(data.state)
            writeBoolean(data.falseStarted)
        }
    }

    @FFResponse(FFOutgoing.NEXT_PLATE)
    fun nextPlate(habboResponse: HabboResponse, data: NextPlateData) {
        habboResponse.apply {
            writeInt(data.seq) // wire: seq antes do playerId (parser §_-Dk§)
            writeInt(data.playerId)
            writeInt(data.plateState)
            writeInt(data.foodTypeId)
            writeInt(data.platePos)
        }
    }

    @FFResponse(FFOutgoing.USE_BIG_PARACHUTE)
    fun useBigParachute(habboResponse: HabboResponse, seq: Int, playerId: Int) {
        habboResponse.apply {
            writeInt(seq) // wire: seq antes do playerId (parser §_-9x§)
            writeInt(playerId)
        }
    }

    @FFResponse(FFOutgoing.LAUNCH_MISSILE)
    fun launchMissile(habboResponse: HabboResponse, seq: Int, shooterId: Int, targetId: Int) {
        habboResponse.apply {
            writeInt(seq) // parser §_-6B§: seq, a, b em ordem de leitura
            writeInt(shooterId)
            writeInt(targetId)
        }
    }

    @FFResponse(FFOutgoing.ENABLE_SHIELD)
    fun enableShield(habboResponse: HabboResponse, seq: Int, playerId: Int, enabled: Boolean) {
        habboResponse.apply {
            writeInt(seq) // wire: seq antes do playerId (parser §_-Ca§)
            writeInt(playerId)
            writeBoolean(enabled)
        }
    }

    @FFResponse(FFOutgoing.END_GAME)
    fun endGame(habboResponse: HabboResponse, data: EndGameData) {
        habboResponse.apply {
            writeInt(data.scores.size)
            data.scores.forEach { (playerId, score) ->
                writeInt(playerId)
                writeInt(score)
            }
        }
    }
}
