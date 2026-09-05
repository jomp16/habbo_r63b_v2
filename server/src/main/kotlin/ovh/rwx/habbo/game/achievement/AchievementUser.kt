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

package ovh.rwx.habbo.game.achievement

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.util.ActivityPointType

data class AchievementUser(
    val id: Int,
    val userId: Int,
    val groupId: Int,
    var level: Int,
    var progress: Int
) : IHabboResponseSerialize {
    val group: AchievementGroup by lazy { HabboServer.habboGame.achievementManager.achievementGroups.values.find { it.id == groupId }!! }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        val levels = HabboServer.habboGame.achievementManager.achievementLevels[groupId] ?: return

        val totalLevels = group.totalLevels
        val isMaxLevel = level >= totalLevels

        var targetLevel = level + 1
        targetLevel = if (targetLevel > totalLevels) totalLevels else targetLevel

        val targetAchievement = levels.find { it.level == targetLevel } ?: levels.lastOrNull() ?: return

        val badgeCode = if (group.badgeAppendLevel) "${group.name}$targetLevel" else group.name
        val scoreAtStart = if (targetLevel == 1) 0 else {
            levels.find { it.level == targetLevel - 1 }?.progressRequirement ?: 0
        }

        val displayMethod = if (isMaxLevel && totalLevels == 1) 1 else 0

        habboResponse.apply {
            writeInt(group.id) // achievementId
            writeInt(targetLevel) // level
            writeUTF(badgeCode) // badgeId
            writeInt(scoreAtStart) // scoreAtStartOfLevel
            writeInt(targetAchievement.progressRequirement) // scoreLimit
            writeInt(targetAchievement.rewardActivityPoints) // levelRewardPoints
            writeInt(ActivityPointType.PIXELS.code) // levelRewardPointType
            writeInt(progress) // currentPoints
            writeBoolean(isMaxLevel) // finalLevel
            writeUTF(group.category.category) // category
            writeUTF("") // subCategory
            writeInt(totalLevels) // levelCount
            writeInt(displayMethod) // displayMethod
            writeShort(0) // state
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        val levels = HabboServer.habboGame.achievementManager.achievementLevels[groupId] ?: return

        val totalLevels = group.totalLevels
        val isMaxLevel = level >= totalLevels

        var targetLevel = level + 1
        targetLevel = if (targetLevel > totalLevels) totalLevels else targetLevel

        val targetAchievement = levels.find { it.level == targetLevel } ?: levels.lastOrNull() ?: return

        val badgeCode = if (group.badgeAppendLevel) "${group.name}$targetLevel" else group.name
        val scoreAtStart = if (targetLevel == 1) 0 else {
            levels.find { it.level == targetLevel - 1 }?.progressRequirement ?: 0
        }

        habboResponse.apply {
            writeInt(group.id)
            writeInt(targetLevel)
            writeUTF(badgeCode)

            if (isVersionAtLeast(2010, 11, 12)) {
                writeInt(targetAchievement.progressRequirement)
                writeInt(targetAchievement.rewardActivityPoints)
                writeInt(ActivityPointType.PIXELS.code)
            }

            if (isVersionAtLeast(2010, 11, 19)) {
                writeInt(progress)
            }

            if (isVersionAtLeast(2011, 5, 6)) {
                writeBoolean(isMaxLevel)
                writeUTF(group.category.category)
                writeInt(totalLevels)
            }
        }
    }

    fun serializeUnlocked(habboResponse: HabboResponse) {
        val levels = HabboServer.habboGame.achievementManager.achievementLevels[groupId] ?: return
        val currentAchievement = levels.find { it.level == level } ?: levels.lastOrNull() ?: return

        habboResponse.apply {
            writeInt(group.id) // type (Achievement ID)
            writeInt(level)    // level alcançado
            writeInt(currentAchievement.id) // badgeId interno (Geralmente 1337)

            val badgeCode = if (group.badgeAppendLevel) "${group.name}$level" else group.name
            writeUTF(badgeCode) // ACH_Name1

            writeInt(currentAchievement.rewardActivityPoints) // points reward
            writeInt(currentAchievement.rewardActivityPoints) // levelRewardPoints
            writeInt(ActivityPointType.PIXELS.code)           // currency type (0 = Pixels)
            writeInt(currentAchievement.rewardAchievementPoints) // bonusPoints / Score
            writeInt(group.id) // achievementID do DB

            val prevBadge = if (level > 1 && group.badgeAppendLevel) {
                "${group.name}${level - 1}"
            } else {
                ""
            }
            writeUTF(prevBadge) // removedBadgeCode (Para a UI substituir o ícone)
            writeUTF(group.category.category) // category
            writeBoolean(true) // showDialogToUser (Alerta na tela)

            if (isVersionAtLeast(2026, 8, 6)) {
                writeInt(HabboServer.habboGame.badgeManager.getOwnerCount(badgeCode)) // ownerCount
                writeInt(0) // todo: badgeRarityId
            }
        }
    }
}