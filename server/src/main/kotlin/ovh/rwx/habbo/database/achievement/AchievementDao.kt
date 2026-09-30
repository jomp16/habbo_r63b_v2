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

package ovh.rwx.habbo.database.achievement

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.db
import ovh.rwx.habbo.database.sequence.HiLoSequence
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.game.achievement.Achievement
import ovh.rwx.habbo.game.achievement.AchievementCategory
import ovh.rwx.habbo.game.achievement.AchievementGroup
import ovh.rwx.habbo.game.achievement.AchievementUser
import java.util.*

object AchievementDao {
    private val log = LoggerFactory.getLogger(javaClass)

    val achievementSequence = HiLoSequence("users_achievements", blockSize = 500)

    fun loadAchievementGroups(): List<Pair<String, AchievementGroup>> = db {
        query<AchievementGroupRowDto>("/sql/achievement/select_achievement_groups.sql")
            .map { it.toDomain() }
    }

    fun loadAchievements(): List<Achievement> = db {
        query<Achievement>("/sql/achievement/select_achievements.sql")
    }

    fun loadUserAchievements(userId: Int): List<AchievementUser> = db {
        query<AchievementUser>(
            "/sql/achievement/select_user_achievements.sql",
            mapOf("user_id" to userId)
        )
    }

    fun loadAllUserAchievements(): Map<Int, List<AchievementUser>> = db {
        query<AchievementUser>(
            "/sql/achievement/select_all_user_achievements.sql"
        ).groupBy { it.userId }
    }

    fun saveUserAchievements(achievementUsers: Collection<AchievementUser>) {
        if (achievementUsers.isEmpty()) return

        db {
            batchUpdate(
                "/sql/achievement/update_user_achievement.sql",
                achievementUsers.map {
                    mapOf(
                        "level" to it.level,
                        "progress" to it.progress,
                        "id" to it.id
                    )
                }
            )
        }
    }

    fun insertUserAchievement(userId: Int, groupId: Int, level: Int, progress: Int): AchievementUser {
        val id = achievementSequence.nextId()
        val achievementUser = AchievementUser(id, userId, groupId, level, progress)
        WriteBehindManager.queue {
            db {
                update(
                    "INSERT INTO `users_achievements` (`id`, `user_id`, `achievement_group_id`, `level`, `progress`) VALUES (:id, :user_id, :achievement_group_id, :level, :progress)",
                    mapOf("id" to id, "user_id" to userId, "achievement_group_id" to groupId, "level" to level, "progress" to progress)
                )
            }
        }
        return achievementUser
    }
}


data class AchievementGroupRowDto(
    val id: Int,
    val name: String,
    val category: String,
    val badgeAppendLevel: Boolean
) {
    fun toDomain() = name to AchievementGroup(
        id,
        name,
        if (category.isEmpty()) AchievementCategory.EMPTY else AchievementCategory.valueOf(category.uppercase(Locale.getDefault())),
        badgeAppendLevel
    )
}
