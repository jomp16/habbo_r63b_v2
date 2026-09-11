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

package ovh.rwx.habbo.communication.outgoing.achievement

import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.achievement.Achievement
import ovh.rwx.habbo.game.achievement.AchievementCategory
import ovh.rwx.habbo.game.achievement.AchievementGroup
import ovh.rwx.habbo.game.achievement.AchievementUser

data class AchievementListData(
    val achievementUsers: List<AchievementUser>,
    val groupedAchievements: Map<AchievementGroup, List<Achievement>>
)

@Suppress("unused", "UNUSED_PARAMETER")
class AchievementListResponse {
    @Response(Outgoing.ACHIEVEMENT_LIST)
    @ResponseR63A(OutgoingR63A.ACHIEVEMENT_LIST)
    fun response(
        habboResponse: HabboResponse,
        data: AchievementListData
    ) {
        habboResponse.apply {
            val targets = if (isVersionBefore(2011, 6, 16)) {
                data.groupedAchievements.filterKeys { it.category != AchievementCategory.EMPTY }
            } else {
                data.groupedAchievements
            }

            writeInt(targets.size)

            targets.forEach { achievementGroupEntry ->
                val userAchievement = data.achievementUsers.find { it.group == achievementGroupEntry.key }
                    ?: AchievementUser(0, 0, achievementGroupEntry.key.id, 0, 0)

                serialize(userAchievement)
            }

            if (isVersionAtLeast(2011, 6, 16)) {
                writeUTF("") // defaultCategory
            }
        }
    }
}