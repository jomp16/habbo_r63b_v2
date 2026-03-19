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
import ovh.rwx.habbo.communication.outgoing.user.ActivityPointType

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

        var targetLevel = level + 1
        if (targetLevel > group.totalLevels) targetLevel = group.totalLevels

        val targetAchievement = levels.find { it.level == targetLevel } ?: levels.lastOrNull() ?: return

        val badgeCode = if (group.badgeAppendLevel) "${group.name}$targetLevel" else group.name
        val scoreAtStart = if (targetLevel == 1) 0 else {
            levels.find { it.level == targetLevel - 1 }?.progressRequirement ?: 0
        }

        habboResponse.apply {
            writeInt(group.id) // achievementId
            writeInt(targetLevel) // level
            writeUTF(badgeCode) // badgeId
            writeInt(scoreAtStart) // scoreAtStartOfLevel
            writeInt(targetAchievement.progressRequirement) // scoreLimit
            writeInt(targetAchievement.rewardActivityPoints) // levelRewardPoints
            writeInt(ActivityPointType.PIXELS.code) // levelRewardPointType
            writeInt(progress) // currentPoints
            writeBoolean(level >= group.totalLevels) // finalLevel
            writeUTF(group.category.category) // category
            writeUTF("") // subCategory
            writeInt(group.totalLevels) // levelCount
            writeInt(0) // displayMethod
            writeShort(0) // state
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        val levels = HabboServer.habboGame.achievementManager.achievementLevels[groupId] ?: return

        var targetLevel = level + 1
        if (targetLevel > group.totalLevels) targetLevel = group.totalLevels

        val targetAchievement = levels.find { it.level == targetLevel } ?: levels.lastOrNull() ?: return

        val badgeCode = if (group.badgeAppendLevel) "${group.name}$targetLevel" else group.name
        val scoreAtStart = if (targetLevel == 1) 0 else {
            levels.find { it.level == targetLevel - 1 }?.progressRequirement ?: 0
        }

        habboResponse.apply {
            writeInt(group.id) // achievementId
            writeInt(targetLevel) // level
            writeUTF(badgeCode) // badgeId
            writeInt(scoreAtStart) // scoreAtStartOfLevel
            writeInt(targetAchievement.progressRequirement) // scoreLimit
            writeInt(targetAchievement.rewardActivityPoints) // levelRewardPoints
            writeInt(ActivityPointType.PIXELS.code) // levelRewardPointType
            writeBoolean(level >= group.totalLevels) // finalLevel
            writeUTF(group.category.category) // category
            writeInt(group.totalLevels) // levelCount
        }
    }
}