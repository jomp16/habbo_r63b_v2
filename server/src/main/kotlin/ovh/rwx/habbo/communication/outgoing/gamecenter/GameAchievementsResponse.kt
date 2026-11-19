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

/** GameAchievementsMessageEvent (2013, 1825): as conquistas de cada jogo, de onde o cliente monta `ACH_<grupo><nível>`. */
@Suppress("unused", "UNUSED_PARAMETER")
class GameAchievementsResponse {
    @Response(Outgoing.GAME_ACHIEVEMENTS)
    fun response(habboResponse: HabboResponse, games: List<GameAchievementsData>) {
        habboResponse.apply {
            writeInt(games.size)
            games.forEach { game ->
                writeInt(game.gameTypeId)
                writeInt(game.achievements.size)
                game.achievements.forEach {
                    writeInt(it.id)
                    writeUTF(it.group)
                    writeInt(it.levels)
                }
            }
        }
    }
}

data class GameAchievementsData(val gameTypeId: Int, val achievements: List<GameAchievement>)

/** [group] é o nome do grupo sem o prefixo `ACH_` (ex. `BaseJumpWins`). */
data class GameAchievement(val id: Int, val group: String, val levels: Int)
