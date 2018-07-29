/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.user.information

import ovh.rwx.habbo.database.achievement.AchievementDao
import ovh.rwx.habbo.database.clothing.ClothingDao
import ovh.rwx.habbo.database.wardrobe.WardrobeDao
import ovh.rwx.habbo.game.achievement.AchievementUser
import ovh.rwx.habbo.game.user.wardrobe.Wardrobe
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

data class UserInformation(
        val id: Int,
        val username: String,
        val email: String,
        val accountCreated: LocalDateTime,
        val realname: String,
        var rank: Int,
        var credits: Int,
        var pixels: Int,
        var vipPoints: Int,
        var figure: String,
        var gender: String,
        var motto: String,
        var homeRoom: Int,
        var vip: Boolean,
        val password: String
) {
    // todo
    val ambassador: Boolean
        get() = rank >= 7
    val wardrobes: MutableList<Wardrobe> = ArrayList(WardrobeDao.getWardrobes(id))
    val clothings: MutableSet<String> = HashSet(ClothingDao.getClothings(id))
    val achievementUsers: MutableList<AchievementUser> = ArrayList(AchievementDao.loadUserAchievements(id))
}