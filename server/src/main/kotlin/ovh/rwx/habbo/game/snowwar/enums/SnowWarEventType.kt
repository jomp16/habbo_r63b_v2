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

package ovh.rwx.habbo.game.snowwar.enums

enum class SnowWarEventType(val id: Int) {
    HUMAN_LEFT(1),
    NEW_MOVE_TARGET(2),
    HUMAN_THROWS_SNOWBALL_AT_HUMAN(3),
    HUMAN_THROWS_SNOWBALL_AT_POSITION(4),
    HUMAN_STARTS_TO_MAKE_A_SNOWBALL(7),
    CREATE_SNOWBALL(8),
    MACHINE_CREATES_SNOWBALL(11),
    HUMAN_GETS_SNOWBALLS_FROM_MACHINE(12);

    companion object {
        fun fromId(id: Int): SnowWarEventType? {
            return entries.firstOrNull { it.id == id }
        }
    }
}
