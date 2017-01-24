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

package tk.jomp16.habbo.database.group

import tk.jomp16.habbo.HabboServer

object GroupDao {
    fun getGroupsBadgesBases(): List<Triple<Int, String, String>> = HabboServer.database {
        select("SELECT * FROM groups_badges_base") {
            Triple(
                    it.int("id"),
                    it.string("value1"),
                    it.string("value2")
            )
        }
    }

    fun getGroupsBadgesSymbols(): List<Triple<Int, String, String>> = HabboServer.database {
        select("SELECT * FROM groups_badges_symbol") {
            Triple(
                    it.int("id"),
                    it.string("value1"),
                    it.string("value2")
            )
        }
    }

    fun getGroupsBadgesBaseColors(): List<Pair<Int, String>> = HabboServer.database {
        select("SELECT * FROM groups_badges_base_color") {
            it.int("id") to it.string("color")
        }
    }

    fun getGroupsBadgesSymbolColors(): List<Pair<Int, String>> = HabboServer.database {
        select("SELECT * FROM groups_badges_symbol_color") {
            it.int("id") to it.string("color")
        }
    }
}