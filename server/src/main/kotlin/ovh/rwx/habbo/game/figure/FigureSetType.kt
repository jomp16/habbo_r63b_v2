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

package ovh.rwx.habbo.game.figure

data class FigureSetType(
    val type: String,
    val paletteId: Int,
    val mandM0: Boolean = false,
    val mandF0: Boolean = false,
    val mandM1: Boolean = false,
    val mandF1: Boolean = false,
    val sets: MutableList<FigureSet> = mutableListOf()
) {
    fun optionalFromClubLevel(gender: String): Int {
        val m0 = if (gender.equals("F", ignoreCase = true)) mandF0 else mandM0
        val m1 = if (gender.equals("F", ignoreCase = true)) mandF1 else mandM1
        return when {
            !m0 -> 0      // Optional for everyone (club and non-club)
            !m1 -> 1      // Mandatory for non-club (0), but optional for club (>= 1)
            else -> -1    // Mandatory for all club levels (cannot be omitted)
        }
    }

    fun isMandatory(gender: String, clubLevel: Int = 0): Boolean {
        val optLevel = optionalFromClubLevel(gender)
        return optLevel == -1 || clubLevel < optLevel
    }

    fun isMandatory(gender: String, club: Boolean): Boolean {
        return isMandatory(gender, if (club) 2 else 0)
    }
}
