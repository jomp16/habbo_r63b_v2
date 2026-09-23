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

package ovh.rwx.habbo.game.item.wired.variable

enum class VariableAvailabilityType(val code: Int) {
    TEMPORARY_ROOM(0),
    TEMPORARY_USER(1),
    PERSISTENT_ROOM(10),
    PERSISTENT_USER(11),
    PERSISTENT_FURNI(20),
    TEMPORARY_FURNI(21),
    NOT_APPLICABLE(999);

    val isPersistent: Boolean
        get() = this == PERSISTENT_ROOM || this == PERSISTENT_USER || this == PERSISTENT_FURNI

    companion object {
        val ROOM = TEMPORARY_ROOM
        val USER = TEMPORARY_USER

        fun fromCode(code: Int): VariableAvailabilityType = entries.firstOrNull { it.code == code } ?: TEMPORARY_ROOM
    }
}