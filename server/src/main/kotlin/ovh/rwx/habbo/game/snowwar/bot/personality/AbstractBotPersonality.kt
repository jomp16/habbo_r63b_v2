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
import ovh.rwx.habbo.game.snowwar.bot.SnowWarBotNavigator
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTrajectory
import java.util.concurrent.ThreadLocalRandom

abstract class AbstractBotPersonality : SnowWarBotPersonality {

    override fun shouldRestock(bot: SnowWarUser, rnd: ThreadLocalRandom): Boolean {
        return bot.snowBallCount == 0 || (bot.snowBallCount < 3 && rnd.nextDouble() < 0.35)
    }

    override fun selectTrajectory(dist: Int, rnd: ThreadLocalRandom): SnowWarTrajectory {
        return when {
            dist < 18_000 -> SnowWarTrajectory.QUICK
            dist < 48_000 -> SnowWarTrajectory.SHORT_LOB
            else -> SnowWarTrajectory.LONG_LOB
        }
    }

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
        return navigator.randomWalkableNeighbor(bot.currentTileX, bot.currentTileY, bot, 3)
    }
}
