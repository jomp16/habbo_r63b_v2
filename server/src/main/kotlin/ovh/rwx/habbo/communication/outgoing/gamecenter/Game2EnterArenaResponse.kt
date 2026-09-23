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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.stuff.LegacyStuffData
import ovh.rwx.habbo.game.item.stuff.MapStuffData
import ovh.rwx.habbo.game.snowwar.SnowWarUser
import ovh.rwx.habbo.game.snowwar.enums.SnowWarUserSerializeMode

@Suppress("unused", "UNUSED_PARAMETER")
class Game2EnterArenaResponse {
    @Response(Outgoing.GAME_2_ENTER_ARENA)
    fun response(
        habboResponse: HabboResponse,
        data: Game2EnterArenaData
    ) {
        habboResponse.apply {
            writeInt(data.gameType)
            writeInt(data.fieldType)
            writeInt(data.numberOfTeams)
            writeInt(data.players.size)
            data.players.forEach { p ->
                serialize(p, SnowWarUserSerializeMode.ARENA)
            }
            writeInt(data.width)
            writeInt(data.height)
            writeUTF(data.heightMap)
            writeInt(data.fuseObjects.size)
            data.fuseObjects.forEach { obj ->
                writeUTF(obj.name)
                writeInt(obj.id)
                writeInt(obj.x)
                writeInt(obj.y)
                writeInt(obj.furnishing?.width ?: 1)
                writeInt(obj.furnishing?.length ?: 1)
                writeInt(obj.furnishing?.stackHeight?.first()?.toInt() ?: 1)
                writeInt(obj.direction)
                writeInt(obj.z)
                writeBoolean(obj.furnishing?.walkable ?: true)
                if (obj.extraParams.isNotEmpty()) {
                    MapStuffData(
                        values = obj.extraParams
                    ).writeFull(habboResponse)
                } else {
                    // LegacyStuffData (format key 0)
                    LegacyStuffData("")
                        .writeFull(habboResponse)
                }
            }
        }
    }
}

data class Game2EnterArenaData(
    val gameType: Int = 0,
    val fieldType: Int = 0,
    val numberOfTeams: Int = 2,
    val players: List<SnowWarUser> = emptyList(),
    val width: Int = 0,
    val height: Int = 0,
    val heightMap: String = "",
    val fuseObjects: List<Game2ArenaFuseObjectData> = emptyList()
)

data class Game2ArenaFuseObjectData(
    val name: String = "",
    val id: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
    val z: Int = 0,
    val direction: Int = 0,
    val extraParams: Map<String, String> = mapOf()
) {
    val furnishing: Furnishing?
        get() = HabboServer.habboGame.itemManager.furnishings[name]
}
