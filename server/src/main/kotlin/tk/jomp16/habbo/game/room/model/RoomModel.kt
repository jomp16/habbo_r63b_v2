/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.game.room.model

import tk.jomp16.habbo.kotlin.array2d
import tk.jomp16.habbo.kotlin.array2dOfShort
import tk.jomp16.habbo.util.Vector3

data class RoomModel(val id: String, val roomId: Int, val doorVector3: Vector3, val doorDir: Int, val heightmap: List<String>, val clubOnly: Boolean) {
    val mapSizeX: Int = heightmap[0].length
    val mapSizeY: Int = heightmap.size
    val squareStates: Array<Array<SquareState>> = array2d(mapSizeX, mapSizeY) { SquareState.CLOSED }
    val floorHeight: Array<ShortArray> = array2dOfShort(mapSizeX, mapSizeY)

    init {
        for (y in 0..mapSizeY - 1) {
            for (x in 0..mapSizeX - 1) {
                val square = heightmap[y][x].toLowerCase()

                if (square == 'x') {
                    squareStates[x][y] = SquareState.CLOSED
                    floorHeight[x][y] = -1
                } else if (Character.isLetterOrDigit(square)) {
                    squareStates[x][y] = SquareState.OPEN
                    floorHeight[x][y] = if (Character.isDigit(square)) square.toString().toShort() else (letters.indexOf(square) + 10).toString().toShort()
                }
            }
        }
    }

    companion object {
        val letters = "abcdefghijklmnopqrstuvw"
    }
}