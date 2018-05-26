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

package ovh.rwx.habbo.communication.outgoing.achievement

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.achievement.Achievement
import ovh.rwx.habbo.game.achievement.AchievementUser

@Suppress("unused", "UNUSED_PARAMETER")
class AchievementListResponse {
    @Response(Outgoing.USER_ACHIEVEMENT)
    fun response(habboResponse: HabboResponse, achievementUsers: List<AchievementUser>, achievements: List<Achievement>) {
        habboResponse.apply {
            writeInt(achievements.size)

            achievements.forEach { achievement ->
                val userAchievement = achievementUsers.find { it.group == achievement.group }

                var targetLevel = (userAchievement?.level?.plus(1)) ?: 1
                val totalLevels = achievement.totalLevels
                targetLevel = (if (targetLevel > totalLevels) totalLevels else targetLevel)
                val targetAchievement = HabboServer.habboGame.achievementManager.groupedAchievements[achievement.group]!!.find { it.level == targetLevel }!!

                writeInt(achievement.id)
                writeInt(targetLevel)
                writeUTF(achievement.group.name + targetLevel)
                writeInt(1)
                writeInt(targetAchievement.progressRequirement)
                writeInt(targetAchievement.rewardActivityPoints)
                writeInt(0) // type of reward
                writeInt(userAchievement?.progress ?: 0)
                writeBoolean((userAchievement?.level ?: 0) >= totalLevels) // is 100% complete
                writeUTF(achievement.category.category)
                writeUTF("")
                writeInt(totalLevels)
                writeInt(0)
            }

            writeUTF("") // ??
        }
    }
}