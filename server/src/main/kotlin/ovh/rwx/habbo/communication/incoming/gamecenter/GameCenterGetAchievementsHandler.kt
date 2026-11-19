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

package ovh.rwx.habbo.communication.incoming.gamecenter

import ovh.rwx.fastfood.game.FastFoodAchievements
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.gamecenter.GameAchievement
import ovh.rwx.habbo.communication.outgoing.gamecenter.GameAchievementsData
import ovh.rwx.habbo.game.gamecenter.GameCenterManager
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class GameCenterGetAchievementsHandler {
    /**
     * O Game Center abriu (997 `_-0-e()`, sem corpo, 2013): pede a configuração de conquistas por jogo, e a resposta é o
     * GameAchievementsMessageEvent (1825). Não é o pedido de status de um jogo: ler um id aqui dava 0, que desde a V33 é
     * o SnowWar, e o servidor mandava "SnowWar ilimitado" por cima do contador do dia.
     */
    @Handler(Incoming.GET_GAME_ACHIEVEMENTS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val manager = HabboServer.habboGame.achievementManager
        val baseJump = listOf(
            FastFoodAchievements.WINS,
            FastFoodAchievements.MISSILE,
            FastFoodAchievements.SHIELD,
            FastFoodAchievements.BIG_PARACHUTE,
            FastFoodAchievements.DAYS_PLAYED
        ).mapNotNull { name ->
            val group = manager.achievementGroups[name] ?: return@mapNotNull null
            GameAchievement(group.id, name.removePrefix("ACH_"), group.totalLevels)
        }
        habboSession.sendHabboResponse(
            Outgoing.GAME_ACHIEVEMENTS,
            listOf(GameAchievementsData(GameCenterManager.BASEJUMP, baseJump))
        )
    }
}
