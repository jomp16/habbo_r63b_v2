/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.util

object Rotation {
    fun calculate(x1: Int, y1: Int, x2: Int, y2: Int) = when {
        x1 > x2 && y1 > y2 -> 7
        x1 < x2 && y1 < y2 -> 3
        x1 > x2 && y1 < y2 -> 5
        x1 < x2 && y1 > y2 -> 1
        x1 > x2            -> 6
        x1 < x2            -> 2
        y1 < y2            -> 4
        y1 > y2            -> 0
        else               -> 0
    }

    fun calculateInverse(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        val rot = calculate(x1, y1, x2, y2)

        return when {
            rot > 3 -> rot - 4
            else    -> rot + 4
        }
    }
}