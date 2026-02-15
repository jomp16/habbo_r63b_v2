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

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.user.ActivityPointType
import ovh.rwx.habbo.game.achievement.Achievement
import ovh.rwx.habbo.game.achievement.AchievementGroup
import ovh.rwx.habbo.game.achievement.AchievementUser

@Suppress("unused", "UNUSED_PARAMETER")
class AchievementListResponse {
    @Response(Outgoing.ACHIEVEMENT_LIST)
    fun response(
        habboResponse: HabboResponse,
        achievementUsers: List<AchievementUser>,
        achievementGroups: Map<String, AchievementGroup>,
        groupedAchievements: Map<AchievementGroup, List<Achievement>>
    ) {
        habboResponse.apply {
            writeInt(achievementGroups.size)

            achievementGroups.values.forEach { achievementGroup ->
                commonStuff(habboResponse, achievementUsers, groupedAchievements, achievementGroup)
            }

            writeUTF("") // defaultCategory
        }
    }

    @ResponseR63A(OutgoingR63A.ACHIEVEMENT_LIST)
    fun responseR63A(
        habboResponse: HabboResponse,
        achievementUsers: List<AchievementUser>,
        achievementGroups: Map<String, AchievementGroup>,
        groupedAchievements: Map<AchievementGroup, List<Achievement>>
    ) {
        habboResponse.apply {
            writeInt(achievementGroups.size)

            achievementGroups.values.forEach { achievementGroup ->
                val userAchievement = achievementUsers.find { it.group == achievementGroup }
                var targetLevel = (userAchievement?.level?.plus(1)) ?: 1
                val totalLevels = achievementGroup.totalLevels
                targetLevel = (if (targetLevel > totalLevels) totalLevels else targetLevel)
                val groupList = groupedAchievements[achievementGroup]
                val targetAchievement = groupList?.find { it.level == targetLevel }
                    ?: groupList?.lastOrNull()
                    ?: return@forEach

                val badgeCode = if (achievementGroup.badgeAppendLevel) {
                    achievementGroup.name + targetLevel
                } else {
                    achievementGroup.name
                }

                writeInt(achievementGroup.id)
                writeInt(targetLevel)
                writeUTF(badgeCode)
                writeInt(targetAchievement.progressRequirement) // scoreLimit
                writeInt(targetAchievement.rewardActivityPoints) // levelRewardPoints
                writeInt(ActivityPointType.PIXELS.code) // levelRewardPointType
                writeInt(userAchievement?.progress ?: 0) // currentPoints
                writeBoolean((userAchievement?.level ?: 0) >= totalLevels) // finalLevel
                writeUTF(achievementGroup.category.category)
                writeInt(totalLevels) // levelCount
            }
        }
    }

    @Response(Outgoing.ACHIEVEMENT_LIST)
    fun responseHabboAir(
        habboResponse: HabboResponse,
        achievementUsers: List<AchievementUser>,
        achievementGroups: Map<String, AchievementGroup>,
        groupedAchievements: Map<AchievementGroup, List<Achievement>>
    ) {
        habboResponse.apply {
            writeInt(achievementGroups.size)

            achievementGroups.values.forEach { achievementGroup ->
                commonStuff(habboResponse, achievementUsers, groupedAchievements, achievementGroup)
                writeShort(0) // state
            }

            writeUTF("") // defaultCategory
        }
    }

    private fun HabboResponse.commonStuff(
        habboResponse: HabboResponse,
        achievementUsers: List<AchievementUser>,
        groupedAchievements: Map<AchievementGroup, List<Achievement>>,
        achievementGroup: AchievementGroup
    ) {
        val userAchievement = achievementUsers.find { it.group == achievementGroup }

        var targetLevel = (userAchievement?.level?.plus(1)) ?: 1
        val totalLevels = achievementGroup.totalLevels
        targetLevel = (if (targetLevel > totalLevels) totalLevels else targetLevel)
        val groupList = groupedAchievements[achievementGroup]
        val targetAchievement = groupList?.find { it.level == targetLevel }
            ?: groupList?.lastOrNull()
            ?: return // Se não tiver NENHUMA conquista no grupo, aborta esse loop (não envia nada desse grupo)
        val badgeCode = if (achievementGroup.badgeAppendLevel) {
            achievementGroup.name + targetLevel // Padrão (ACH_Login1)
        } else {
            achievementGroup.name // Estático (ACH_VipParties2_Entry)
        }
        // Se for nível 1, começa do 0. Se for nível 2, começa onde o nível 1 terminou.
        val scoreAtStart = if (targetLevel == 1) 0 else {
            // Pega o achievement do nível anterior para saber onde ele terminava
            groupedAchievements[achievementGroup]?.find { it.level == targetLevel - 1 }?.progressRequirement ?: 0
        }

        writeInt(achievementGroup.id)
        writeInt(targetLevel)
        writeUTF(badgeCode) // Envia o código corrigido
        writeInt(scoreAtStart) // <--- Corrigido (Envia 0 para lvl 1, 20 para lvl 2, etc)
        writeInt(targetAchievement.progressRequirement)
        writeInt(targetAchievement.rewardActivityPoints)
        writeInt(ActivityPointType.PIXELS.code) // type of reward
        writeInt(userAchievement?.progress ?: 0)
        writeBoolean((userAchievement?.level ?: 0) >= totalLevels) // is 100% complete
        writeUTF(achievementGroup.category.category)
        writeUTF("") // subCategory
        writeInt(totalLevels)
        writeInt(0) // displayMethod
    }
}