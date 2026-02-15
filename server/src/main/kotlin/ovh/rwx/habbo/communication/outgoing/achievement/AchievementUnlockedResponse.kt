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
import ovh.rwx.habbo.game.achievement.AchievementUser

@Suppress("unused", "UNUSED_PARAMETER")
class AchievementUnlockedResponse {
    @Response(Outgoing.ACHIEVEMENT_UNLOCKED)
    @ResponseR63A(OutgoingR63A.ACHIEVEMENT_UNLOCKED)
    fun response(habboResponse: HabboResponse, achievementUser: AchievementUser, achievement: Achievement) {
        habboResponse.apply {
            writeInt(achievementUser.group.id) // type - Grupo ID
            writeInt(achievementUser.level) // level - Nível alcançado
            writeInt(achievement.id) // badgeId - ID interno do badge
            val badgeCode = if (achievementUser.group.badgeAppendLevel) {
                "${achievementUser.group.name}${achievementUser.level}"
            } else {
                achievementUser.group.name
            }
            writeUTF(badgeCode) // badgeCode - Código do badge (ACH_Name1)
            writeInt(achievement.rewardActivityPoints) // points - Recompensa de Pixels/Duckets
            writeInt(achievement.rewardActivityPoints) // levelRewardPoints - Repete a recompensa
            writeInt(ActivityPointType.PIXELS.code) // levelRewardPointType - Tipo de moeda (0 = Pixels)
            writeInt(achievement.rewardAchievementPoints) // bonusPoints - Score/Topázios
            writeInt(achievement.id) // achievementID - ID único da linha no banco
            val prevBadge = if (achievementUser.level > 1 && achievementUser.group.badgeAppendLevel) {
                "${achievementUser.group.name}${achievementUser.level - 1}"
            } else {
                ""
            }
            writeUTF(prevBadge) // removedBadgeCode - Badge anterior para remover
            writeUTF(achievementUser.group.category.category) // category - Categoria
            writeBoolean(true) // showDialogToUser - Mostrar alerta na tela
        }
    }
}
