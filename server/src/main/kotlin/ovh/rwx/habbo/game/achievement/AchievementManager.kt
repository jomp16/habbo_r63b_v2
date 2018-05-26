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

package ovh.rwx.habbo.game.achievement

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.achievement.AchievementDao
import ovh.rwx.habbo.game.user.HabboSession

class AchievementManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val achievementGroups: MutableMap<String, AchievementGroup> = mutableMapOf()
    private val achievements: MutableList<Achievement> = mutableListOf()
    val groupedAchievements: Map<AchievementGroup, List<Achievement>>
        get() = achievements.groupBy { it.group }

    fun load() {
        log.info("Loading achievements...")

        achievementGroups.clear()
        achievements.clear()

        achievementGroups += AchievementDao.loadAchievementGroups()
        achievements += AchievementDao.loadAchievements()

        log.info("Loaded {} achievement groups!", achievementGroups.size)
        log.info("Loaded {} achievements!", achievements.size)
    }

    fun progressAchievement(habboSession: HabboSession, achievementGroup: AchievementGroup) {

    }
}