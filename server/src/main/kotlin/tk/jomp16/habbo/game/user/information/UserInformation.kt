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

package tk.jomp16.habbo.game.user.information

import tk.jomp16.habbo.database.wardrobe.WardrobeDao
import tk.jomp16.habbo.game.user.wardrobe.Wardrobe
import java.util.*

data class UserInformation(
        val id: Int,
        val username: String,
        val email: String,
        val realname: String,
        var rank: Int,
        var credits: Int,
        var pixels: Int,
        var vipPoints: Int,
        var figure: String,
        var gender: String,
        var motto: String,
        var homeRoom: Int,
        var vip: Boolean
                          ) {
    // todo
    val ambassador: Boolean
        get() = rank >= 7

    val wardrobes: MutableList<Wardrobe> = ArrayList(WardrobeDao.getWardrobes(id))
}