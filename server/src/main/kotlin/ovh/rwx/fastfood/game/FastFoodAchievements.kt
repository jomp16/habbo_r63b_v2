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

package ovh.rwx.fastfood.game

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.user.HabboSession

/**
 * As conquistas `ACH_BaseJump*` (V36). O cliente casa o nível de cada uma pelo nome sem o prefixo `ACH_`
 * (`Player.§_-7l§("BaseJumpWins")`) e escolhe o kit do paraquedas (L5_/L10_/L15_/L20_) pelo nível de `BaseJumpWins`.
 */
object FastFoodAchievements {
    const val WINS = "ACH_BaseJumpWins"
    const val MISSILE = "ACH_BaseJumpMissile"
    const val SHIELD = "ACH_BaseJumpShield"
    const val BIG_PARACHUTE = "ACH_BaseJumpBigParachute"
    const val DAYS_PLAYED = "ACH_BaseJumpDaysPlayed"

    /** O que o GAME_STARTING manda por jogador: nome sem `ACH_` e nível atual (0 sem progresso). */
    val WIRE_NAMES = listOf(WINS, MISSILE, SHIELD, BIG_PARACHUTE).map { it.removePrefix("ACH_") }

    fun levelsOf(habboSession: HabboSession): List<Pair<String, Int>> {
        val manager = HabboServer.habboGame.achievementManager
        val owned = habboSession.userInformation.achievementUsers
        return WIRE_NAMES.map { wire ->
            val groupId = manager.achievementGroups["ACH_$wire"]?.id
            wire to (owned.find { it.groupId == groupId }?.level ?: 0)
        }
    }

    fun progress(player: FastFoodGamePlayer, name: String, amount: Int = 1) {
        val habboSession = player.session?.habboSession?.takeIf { !player.isBot } ?: return
        HabboServer.habboGame.achievementManager.progress(habboSession, name, amount, accumulate = true)
    }
}
