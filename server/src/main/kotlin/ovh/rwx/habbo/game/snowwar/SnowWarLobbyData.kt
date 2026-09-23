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

package ovh.rwx.habbo.game.snowwar

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.game.snowwar.enums.SnowWarUserSerializeMode

data class SnowWarLobbyData(
    val gameId: Int,
    val levelName: String,
    val gameType: Int = 0,
    val fieldType: Int = 0,
    val numberOfTeams: Int = 2,
    val maximumPlayers: Int = 10,
    val owningPlayerName: String = "",
    val levelEntryId: Int = 0,
    val players: List<SnowWarUser> = emptyList()
) : IHabboResponseSerialize {
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(gameId)
            writeUTF(levelName)
            writeInt(gameType)
            writeInt(fieldType)
            writeInt(numberOfTeams)
            writeInt(maximumPlayers)
            writeUTF(owningPlayerName)
            writeInt(levelEntryId)
            writeInt(players.size)
            players.forEach { p ->
                serialize(p, SnowWarUserSerializeMode.LOBBY)
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        serializeHabboResponse(habboResponse, *params)
    }
}
