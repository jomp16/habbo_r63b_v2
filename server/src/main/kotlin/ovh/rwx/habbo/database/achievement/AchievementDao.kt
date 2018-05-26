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

package ovh.rwx.habbo.database.achievement

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.achievement.Achievement
import ovh.rwx.habbo.game.achievement.AchievementCategory
import ovh.rwx.habbo.game.achievement.AchievementGroup
import ovh.rwx.habbo.game.achievement.AchievementUser

object AchievementDao {
    fun loadAchievementGroups(): List<Pair<String, AchievementGroup>> {
        return HabboServer.database {
            select(javaClass.getResource("/sql/achievement/select_achievement_groups.sql").readText()) {
                it.string("name") to AchievementGroup(
                        it.int("id"),
                        it.string("name")
                )
            }
        }
    }

    fun loadAchievements(): List<Achievement> {
        return HabboServer.database {
            select(javaClass.getResource("/sql/achievement/select_achievements.sql").readText()) {
                Achievement(
                        it.int("id"),
                        it.int("achievement_group_id"),
                        AchievementCategory.valueOf(it.string("category").toUpperCase()),
                        it.int("level"),
                        it.int("reward_activity_points"),
                        it.int("reward_achievement_points"),
                        it.int("progress_requirement"),
                        it.boolean("enabled")
                )
            }
        }
    }

    fun loadUserAchievements(userId: Int): List<AchievementUser> {
        return HabboServer.database {
            select(javaClass.getResource("/sql/achievement/select_user_achievements.sql").readText(),
                    mapOf(
                            "user_id" to userId
                    )
            ) {
                AchievementUser(
                        it.int("id"),
                        it.int("user_id"),
                        it.int("achievement_group_id"),
                        it.int("level"),
                        it.int("progress")
                )
            }
        }
    }
}