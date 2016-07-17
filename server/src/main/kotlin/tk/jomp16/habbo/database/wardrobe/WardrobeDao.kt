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

package tk.jomp16.habbo.database.wardrobe

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.user.wardrobe.Wardrobe

object WardrobeDao {
    fun getWardrobes(userId: Int) = HabboServer.database {
        select("SELECT * FROM users_wardrobe WHERE user_id = :user_id",
               mapOf(
                       "user_id" to userId
               )
        ) {
            Wardrobe(
                    it.int("id"),
                    it.int("slot_id"),
                    it.string("figure"),
                    it.string("gender")
            )
        }
    }
}