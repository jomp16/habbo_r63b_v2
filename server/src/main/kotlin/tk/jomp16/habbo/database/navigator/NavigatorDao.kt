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

package tk.jomp16.habbo.database.navigator

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.navigator.NavigatorEventCategory
import tk.jomp16.habbo.game.navigator.NavigatorRoomCategory

object NavigatorDao {
    fun getNavigatorRoomCategories(): List<NavigatorRoomCategory> = HabboServer.database {
        select("SELECT * FROM navigator_room_categories WHERE enabled = :enabled",
                mapOf(
                        "enabled" to true
                )
        ) {
            NavigatorRoomCategory(
                    it.int("id"),
                    it.string("caption"),
                    it.int("min_rank")
            )
        }
    }

    fun getNavigatorEventCategories(): List<NavigatorEventCategory> = HabboServer.database {
        select("SELECT * FROM navigator_event_categories WHERE visible = :visible",
                mapOf(
                        "visible" to true
                )
        ) {
            NavigatorEventCategory(
                    it.int("id"),
                    it.string("caption"),
                    it.int("min_rank")
            )
        }
    }
}