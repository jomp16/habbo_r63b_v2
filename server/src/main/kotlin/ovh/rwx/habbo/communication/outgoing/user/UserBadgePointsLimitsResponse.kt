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

package ovh.rwx.habbo.communication.outgoing.user

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.achievement.Achievement
import ovh.rwx.habbo.game.achievement.AchievementGroup

@Suppress("unused", "UNUSED_PARAMETER")
class UserBadgePointsLimitsResponse {
    @Response(Outgoing.BADGE_POINTS_LIMIT)
    @ResponseR63A(OutgoingR63A.BADGE_POINTS_LIMIT)
    fun response(habboResponse: HabboResponse, groupedAchievements: Map<AchievementGroup, List<Achievement>>) {
        habboResponse.apply {
            // 1. Quantidade de Grupos (Chaves do Map)
            writeInt(groupedAchievements.size)

            // Itera sobre cada entrada do Map (Chave = Grupo, Valor = Lista de Níveis)
            for ((group, levels) in groupedAchievements) {

                // 2. Nome do Grupo (ex: ACH_Login)
                writeUTF(group.name.removePrefix("ACH_"))

                // Ordena os níveis para garantir a sequência (1, 2, 3...)
                // É crucial ordenar, pois o groupBy pode não garantir ordem
                val sortedLevels = levels.sortedBy { it.level }

                // 3. Quantidade de Níveis neste Grupo
                writeInt(sortedLevels.size)

                for (achievement in sortedLevels) {
                    // 4. Par {Nível, Limite}
                    writeInt(achievement.level)               // Ex: 1
                    writeInt(achievement.progressRequirement) // Ex: 5 (Valor do %limit%)
                }
            }
        }
    }
}