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
import ovh.rwx.fastfood.game.FastFoodFallingObjectType
import ovh.rwx.fastfood.game.FastFoodGamePlayer
import ovh.rwx.habbo.communication.HabboResponse

@Suppress("unused", "UNUSED_PARAMETER")
class GameStartingResponse {
    @FFResponse(FFOutgoing.GAME_STARTING)
    fun response(
        habboResponse: HabboResponse,
        myPlayerId: Int,
        boolA: Boolean,
        boolB: Boolean,
        boolC: Boolean,
        firstFoodTypeId: Int,
        fallingObjectTypes: List<FastFoodFallingObjectType>,
        plateTargets: Map<Int, Int>,
        players: List<FastFoodGamePlayer>
    ) {
        habboResponse.apply {
            writeInt(myPlayerId)
            writeBoolean(boolA)
            writeBoolean(boolB)
            writeBoolean(boolC)

            writeInt(firstFoodTypeId) // §_-An§/§_-JJ§: o tipo do primeiro prato (BaseJumpEngine.§_-64§ param3)

            writeInt(fallingObjectTypes.size)
            fallingObjectTypes.forEach {
                writeInt(it.id)
                writeUTF(it.gravity)
                writeUTF(it.parachuteDecel)
                writeUTF(it.minFallSpeed)
                writeUTF(it.bigParachuteMinFallSpeed)
                writeInt(it.plateTimerTicks)
            }

            // powerups da partida por tipo (0 paraquedas grande, 1 míssil, 2 escudo: §_-DQ§), decrementados pelo §_-4p§
            writeInt(plateTargets.size)
            plateTargets.forEach { (type, count) ->
                writeInt(type)
                writeInt(count)
            }

            writeInt(players.size)
            players.forEach { player ->
                writeInt(player.id)
                writeUTF(player.name)
                writeUTF("")            // §_-9v§: URL de retrato custom — vazio usa o sprite do BasicAssets
                writeUTF(player.gender) // §_-ME§: M/F → male_normal_png / female_normal_png
                writeUTF("br")           // §_-Hd§: código do país (bandeira ff_flag_XX_png)
                // conquistas (§_-0N§): nome sem ACH_, nível, url; BaseJumpWins define a cor do paraquedas
                writeInt(player.achievementLevels.size)
                player.achievementLevels.forEach { (name, level) ->
                    writeUTF(name)
                    writeInt(level)
                    writeUTF("")
                }
            }
        }
    }
}
