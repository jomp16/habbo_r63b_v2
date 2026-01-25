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

package ovh.rwx.habbo.game.room.tasks

import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.IRoomTask
import ovh.rwx.habbo.game.room.Room

class BattleBanzaiTilesFlickerTask(
    private val tiles: List<RoomItem>,
    private val teamColor: Int
) : IRoomTask {
    private var on = false
    private var count = 0

    override fun executeTask(room: Room) {
        val state = if (on) {
            on = false
            (teamColor * 3) + 2 // Estado locked
        } else {
            on = true
            0 // Apagado
        }

        tiles.forEach { tile ->
            tile.extraData = state.toString()
            tile.update(updateDb = false, updateClient = true)
        }

        count++

        if (count < 9) {
            // Agenda próxima execução (500ms = 1 tick)
            room.roomTask?.addTask(room, this)
        } else {
            // Após 9 ciclos, reseta os tiles para o estado final locked
            tiles.forEach { tile ->
                tile.extraData = ((teamColor * 3) + 2).toString()
                tile.update(updateDb = false, updateClient = true)
            }
        }
    }
}
