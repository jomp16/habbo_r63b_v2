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

package ovh.rwx.habbo.game.user.information

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.achievement.AchievementDao
import ovh.rwx.habbo.database.clothing.ClothingDao
import ovh.rwx.habbo.database.wardrobe.WardrobeDao
import ovh.rwx.habbo.database.writebehind.AbstractDirtyEntity
import ovh.rwx.habbo.game.achievement.AchievementUser
import ovh.rwx.habbo.game.group.Group
import ovh.rwx.habbo.game.user.wardrobe.Wardrobe
import ovh.rwx.habbo.util.ActivityPointType
import java.time.LocalDateTime

class UserInformation(
    val id: Int,
    val username: String,
    val email: String,
    val accountCreated: LocalDateTime,
    val realname: String,
    rank: Int,
    credits: Int,
    figure: String,
    gender: String,
    motto: String,
    homeRoom: Int,
    vip: Boolean,
    val password: String,
    val activityPointsCurrencies: MutableMap<ActivityPointType, Int>
) : AbstractDirtyEntity() {
    var rank: Int = rank; set(v) { if (field != v) { field = v; markDirty() } }
    var credits: Int = credits; set(v) { if (field != v) { field = v; markDirty() } }
    var figure: String = figure; set(v) { if (field != v) { field = v; markDirty() } }
    var gender: String = gender; set(v) { if (field != v) { field = v; markDirty() } }
    var motto: String = motto; set(v) { if (field != v) { field = v; markDirty() } }
    var homeRoom: Int = homeRoom; set(v) { if (field != v) { field = v; markDirty() } }
    var vip: Boolean = vip; set(v) { if (field != v) { field = v; markDirty() } }

    val ambassador: Boolean
        get() = rank >= 7
    val wardrobes: MutableList<Wardrobe> by lazy { ArrayList(WardrobeDao.getWardrobes(id)) }
    val clothings: MutableSet<String> by lazy { HashSet(ClothingDao.getClothings(id)) }
    val achievementUsers: MutableList<AchievementUser> by lazy { ArrayList(AchievementDao.loadUserAchievements(id)) }
    val groups: List<Group>
        get() = HabboServer.habboGame.groupManager.groups.values.filter { it.members.any { groupMember -> groupMember.userId == id } }

    override fun flush() {
        ovh.rwx.habbo.database.user.UserInformationDao.saveInformation(this, online = true, ip = "")
        markClean()
    }
}