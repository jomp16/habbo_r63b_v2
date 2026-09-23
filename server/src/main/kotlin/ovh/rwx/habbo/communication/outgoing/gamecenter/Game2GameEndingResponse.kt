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
import ovh.rwx.habbo.game.snowwar.SnowWarUser
import ovh.rwx.habbo.game.snowwar.enums.SnowWarUserSerializeMode

@Suppress("unused", "UNUSED_PARAMETER")
class Game2GameEndingResponse {
    @Response(Outgoing.GAME_2_GAME_ENDING)
    fun response(
        habboResponse: HabboResponse,
        data: Game2GameEndingData
    ) {
        habboResponse.apply {
            writeInt(data.timeToNextState)
            // Game2GameResult
            writeBoolean(data.isDeathMatch)
            writeInt(data.resultType)
            writeInt(data.winnerId)
            // Teams scores
            writeInt(data.teams.size)
            for (team in data.teams) {
                writeInt(team.teamReference)
                writeInt(team.score)
                writeInt(team.players.size)
                for (p in team.players) {
                    serialize(p, SnowWarUserSerializeMode.TEAM_ENDING)
                }
            }
            // Game2SnowWarGameStats
            writeInt(data.playerWithMostKills)
            writeInt(data.playerWithMostHits)
        }
    }
}

data class Game2GameEndingData(
    val timeToNextState: Int = 10,
    val isDeathMatch: Boolean = false,
    val resultType: Int = 0,
    val winnerId: Int = 0,
    val teams: List<Game2TeamScoreData> = emptyList(),
    val playerWithMostKills: Int = 0,
    val playerWithMostHits: Int = 0
)

data class Game2TeamScoreData(
    val teamReference: Int = 1,
    val score: Int = 0,
    val players: List<SnowWarUser> = emptyList()
)
