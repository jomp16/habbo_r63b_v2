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
import ovh.rwx.habbo.communication.isVersionAtLeast
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
        groupedAchievements: Map<AchievementGroup, List<Achievement>>
    ) {
        habboResponse.apply {
            writeInt(groupedAchievements.size)

            groupedAchievements.forEach { achievementGroup ->
                commonStuff(habboResponse, achievementUsers, achievementGroup)
            }

            writeUTF("") // defaultCategory
        }
    }

    @ResponseR63A(OutgoingR63A.ACHIEVEMENT_LIST)
    fun responseR63A(
        habboResponse: HabboResponse,
        achievementUsers: List<AchievementUser>,
        groupedAchievements: Map<AchievementGroup, List<Achievement>>
    ) {
        habboResponse.apply {
            writeInt(groupedAchievements.size)

            groupedAchievements.forEach { achievementGroupEntry ->
                val userAchievement = achievementUsers.find { it.group == achievementGroupEntry.key }

                val totalLevels = achievementGroupEntry.key.totalLevels
                val isMaxLevel = (userAchievement?.level ?: 0) >= totalLevels

                var targetLevel = (userAchievement?.level?.plus(1)) ?: 1
                targetLevel = (if (targetLevel > totalLevels) totalLevels else targetLevel)

                val targetAchievement = achievementGroupEntry.value.find { it.level == targetLevel }
                    ?: achievementGroupEntry.value.lastOrNull()
                    ?: return@forEach // Evita crash se o grupo estiver vazio

                val badgeCode = if (achievementGroupEntry.key.badgeAppendLevel) {
                    achievementGroupEntry.key.name + targetLevel // Padrão (ACH_Login1)
                } else {
                    achievementGroupEntry.key.name // Estático (ACH_VipParties2_Entry)
                }

                writeInt(achievementGroupEntry.key.id)
                writeInt(targetLevel)
                writeUTF(badgeCode)
                writeInt(targetAchievement.progressRequirement) // scoreLimit
                writeInt(targetAchievement.rewardActivityPoints) // levelRewardPoints
                writeInt(ActivityPointType.PIXELS.code) // levelRewardPointType
                writeInt(userAchievement?.progress ?: 0) // currentPoints

                if (isVersionAtLeast(2011, 5, 6)) {
                    writeBoolean(isMaxLevel) // finalLevel
                    writeUTF(achievementGroupEntry.key.category.category)
                    writeInt(totalLevels) // levelCount
                }
            }

            if (isVersionAtLeast(2011, 6, 16)) {
                writeUTF("") // defaultCategory
            }
        }
    }

    @Response(Outgoing.ACHIEVEMENT_LIST)
    fun responseHabboAir(
        habboResponse: HabboResponse,
        achievementUsers: List<AchievementUser>,
        groupedAchievements: Map<AchievementGroup, List<Achievement>>
    ) {
        habboResponse.apply {
            writeInt(groupedAchievements.size)

            groupedAchievements.forEach { achievementGroupEntry ->
                commonStuff(habboResponse, achievementUsers, achievementGroupEntry)
                writeShort(0) // state
            }

            writeUTF("") // defaultCategory
        }
    }

    private fun HabboResponse.commonStuff(
        habboResponse: HabboResponse,
        achievementUsers: List<AchievementUser>,
        achievementGroupEntry: Map.Entry<AchievementGroup, List<Achievement>>
    ) {
        val userAchievement = achievementUsers.find { it.group == achievementGroupEntry.key }

        val totalLevels = achievementGroupEntry.key.totalLevels
        val isMaxLevel = (userAchievement?.level ?: 0) >= totalLevels

        var targetLevel = (userAchievement?.level?.plus(1)) ?: 1
        targetLevel = (if (targetLevel > totalLevels) totalLevels else targetLevel)
        val targetAchievement = achievementGroupEntry.value.find { it.level == targetLevel }
            ?: achievementGroupEntry.value.lastOrNull()
            ?: return // Se não tiver NENHUMA conquista no grupo, aborta esse loop (não envia nada desse grupo)
        val badgeCode = if (achievementGroupEntry.key.badgeAppendLevel) {
            achievementGroupEntry.key.name + targetLevel // Padrão (ACH_Login1)
        } else {
            achievementGroupEntry.key.name // Estático (ACH_VipParties2_Entry)
        }
        // Se for nível 1, começa do 0. Se for nível 2, começa onde o nível 1 terminou.
        val scoreAtStart = if (targetLevel == 1) 0 else {
            // Pega o achievement do nível anterior para saber onde ele terminava
            achievementGroupEntry.value.find { it.level == targetLevel - 1 }?.progressRequirement ?: 0
        }
        val displayMethod = if (isMaxLevel && totalLevels == 1) 1 else 0

        writeInt(achievementGroupEntry.key.id)
        writeInt(targetLevel)
        writeUTF(badgeCode) // Envia o código corrigido
        writeInt(scoreAtStart) // <--- Corrigido (Envia 0 para lvl 1, 20 para lvl 2, etc)
        writeInt(targetAchievement.progressRequirement)
        writeInt(targetAchievement.rewardActivityPoints)
        writeInt(ActivityPointType.PIXELS.code) // type of reward
        writeInt(userAchievement?.progress ?: 0)
        writeBoolean(isMaxLevel) // is 100% complete
        writeUTF(achievementGroupEntry.key.category.category)
        writeUTF("") // subCategory
        writeInt(totalLevels)
        writeInt(displayMethod) // displayMethod
    }
}