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

package ovh.rwx.habbo.util

import kotlin.math.abs

enum class Direction(val code: Int) {
    NORTH(0),
    NORTH_EAST(1),
    EAST(2),
    SOUTH_EAST(3),
    SOUTH(4),
    SOUTH_WEST(5),
    WEST(6),
    NORTH_WEST(7);

    fun turnRight45() = fromCode((code + 1) % 8)
    fun turnRight90() = fromCode((code + 2) % 8)
    fun turnLeft45() = fromCode(if (code - 1 < 0) 7 else code - 1)
    fun turnLeft90() = fromCode(if (code - 2 < 0) code + 6 else code - 2)
    fun turnAround() = fromCode((code + 4) % 8)

    fun getOffset(): Pair<Int, Int> = when (this) {
        NORTH -> Pair(0, -1)
        NORTH_EAST -> Pair(1, -1)
        EAST -> Pair(1, 0)
        SOUTH_EAST -> Pair(1, 1)
        SOUTH -> Pair(0, 1)
        SOUTH_WEST -> Pair(-1, 1)
        WEST -> Pair(-1, 0)
        NORTH_WEST -> Pair(-1, -1)
    }

    companion object {
        fun fromCode(code: Int) = values().find { it.code == code } ?: NORTH

        fun calculate(x1: Int, y1: Int, x2: Int, y2: Int) = when {
            x1 > x2 && y1 > y2 -> 7
            x1 < x2 && y1 < y2 -> 3
            x1 > x2 && y1 < y2 -> 5
            x1 < x2 && y1 > y2 -> 1
            x1 > x2 -> 6
            x1 < x2 -> 2
            y1 < y2 -> 4
            y1 > y2 -> 0
            else -> 0
        }

        @Suppress("unused")
        fun calculateInverse(x1: Int, y1: Int, x2: Int, y2: Int): Int {
            val rot = calculate(x1, y1, x2, y2)

            return when {
                rot > 3 -> rot - 4
                else -> rot + 4
            }
        }

        fun rotationDistance(rot1: Int, rot2: Int): Int {
            val diff = abs(rot1 - rot2)
            return minOf(diff, 8 - diff)
        }
    }
}
