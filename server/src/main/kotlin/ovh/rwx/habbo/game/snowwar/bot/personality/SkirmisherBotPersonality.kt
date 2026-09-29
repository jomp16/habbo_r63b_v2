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

package ovh.rwx.habbo.game.snowwar.bot.personality

import ovh.rwx.habbo.game.snowwar.SnowWarUser
import ovh.rwx.habbo.game.snowwar.bot.BotRole
import ovh.rwx.habbo.game.snowwar.bot.SnowWarBotNavigator
import java.util.concurrent.ThreadLocalRandom

class SkirmisherBotPersonality : AbstractBotPersonality() {
    override val role: BotRole = BotRole.SKIRMISHER

    override fun getDecisionCooldown(rnd: ThreadLocalRandom): Int = rnd.nextInt(5, 11) // 0.7s - 1.6s

    override fun decideMovement(
        bot: SnowWarUser,
        closestEnemy: SnowWarUser?,
        distToEnemy: Int,
        navigator: SnowWarBotNavigator,
        hasSnowballs: Boolean,
        rnd: ThreadLocalRandom
    ): Pair<Int, Int>? {
        if (closestEnemy == null) {
            return navigator.randomWalkableNeighbor(bot.currentTileX, bot.currentTileY, bot, 4)
        }

        // Flanks often, closes in if far, steps back if too close
        return when {
            rnd.nextDouble() < 0.50 -> navigator.flankEnemy(
                bot.currentTileX,
                bot.currentTileY,
                closestEnemy.currentTileX,
                closestEnemy.currentTileY,
                bot
            )

            distToEnemy > 45_000 -> navigator.stepTowards(
                bot.currentTileX,
                bot.currentTileY,
                closestEnemy.currentTileX,
                closestEnemy.currentTileY,
                bot,
                3
            )

            else -> navigator.stepAway(
                bot.currentTileX,
                bot.currentTileY,
                closestEnemy.currentTileX,
                closestEnemy.currentTileY,
                bot,
                2
            )
        }
    }
}
