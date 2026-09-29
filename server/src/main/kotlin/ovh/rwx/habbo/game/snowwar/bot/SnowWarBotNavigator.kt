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

package ovh.rwx.habbo.game.snowwar.bot

import ovh.rwx.habbo.game.snowwar.SnowWarGame
import ovh.rwx.habbo.game.snowwar.SnowWarUser
import ovh.rwx.habbo.game.snowwar.utils.SnowWarMath
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs
import kotlin.math.sign

class SnowWarBotNavigator(private val game: SnowWarGame) {

    fun isAdjacent(x1: Int, y1: Int, x2: Int, y2: Int): Boolean {
        return abs(x1 - x2) <= 1 && abs(y1 - y2) <= 1
    }

    fun findWalkableAdjacentTile(centerX: Int, centerY: Int, bot: SnowWarUser): Pair<Int, Int>? {
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val tx = centerX + dx
                val ty = centerY + dy
                if (game.isTileWalkable(tx, ty, bot)) {
                    candidates.add(tx to ty)
                }
            }
        }
        if (candidates.isEmpty()) return null
        return candidates.minByOrNull { abs(it.first - bot.currentTileX) + abs(it.second - bot.currentTileY) }
    }

    fun findDodgeTile(bot: SnowWarUser, ballDirection360: Int): Pair<Int, Int>? {
        val dir8 = SnowWarMath.direction360To8(ballDirection360)
        val leftDodgeDir = (dir8 + 2) % 8
        val rightDodgeDir = (dir8 + 6) % 8

        val offsetX = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
        val offsetY = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)

        val candidates = listOf(
            (bot.currentTileX + offsetX[leftDodgeDir] * 2) to (bot.currentTileY + offsetY[leftDodgeDir] * 2),
            (bot.currentTileX + offsetX[rightDodgeDir] * 2) to (bot.currentTileY + offsetY[rightDodgeDir] * 2),
            (bot.currentTileX + offsetX[leftDodgeDir]) to (bot.currentTileY + offsetY[leftDodgeDir]),
            (bot.currentTileX + offsetX[rightDodgeDir]) to (bot.currentTileY + offsetY[rightDodgeDir])
        )

        return candidates.firstOrNull { game.isTileWalkable(it.first, it.second, bot) }
    }

    fun flankEnemy(fromX: Int, fromY: Int, toX: Int, toY: Int, bot: SnowWarUser): Pair<Int, Int>? {
        val dx = toX - fromX
        val dy = toY - fromY
        // Perpendicular vector for flanking around the side
        val perpX = -dy.sign * 3
        val perpY = dx.sign * 3
        val targetX = fromX + perpX + dx.sign * 2
        val targetY = fromY + perpY + dy.sign * 2
        if (game.isTileWalkable(targetX, targetY, bot)) return targetX to targetY
        return stepTowards(fromX, fromY, toX, toY, bot, 2)
    }

    fun stepTowards(fromX: Int, fromY: Int, toX: Int, toY: Int, bot: SnowWarUser, stepSize: Int = 3): Pair<Int, Int>? {
        val dx = (toX - fromX).sign
        val dy = (toY - fromY).sign
        for (step in stepSize downTo 1) {
            val stepX = fromX + dx * step
            val stepY = fromY + dy * step
            if (game.isTileWalkable(stepX, stepY, bot)) return stepX to stepY
        }
        return randomWalkableNeighbor(fromX, fromY, bot, 2)
    }

    fun stepAway(
        fromX: Int,
        fromY: Int,
        enemyX: Int,
        enemyY: Int,
        bot: SnowWarUser,
        stepSize: Int = 3
    ): Pair<Int, Int>? {
        val dx = -(enemyX - fromX).sign
        val dy = -(enemyY - fromY).sign
        for (step in stepSize downTo 1) {
            val stepX = fromX + dx * step
            val stepY = fromY + dy * step
            if (game.isTileWalkable(stepX, stepY, bot)) return stepX to stepY
        }
        return randomWalkableNeighbor(fromX, fromY, bot, 2)
    }

    fun randomWalkableNeighbor(fromX: Int, fromY: Int, bot: SnowWarUser, radius: Int = 3): Pair<Int, Int>? {
        val rnd = ThreadLocalRandom.current()
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                if (dx == 0 && dy == 0) continue
                val tx = fromX + dx
                val ty = fromY + dy
                if (game.isTileWalkable(tx, ty, bot)) {
                    candidates.add(tx to ty)
                }
            }
        }
        if (candidates.isEmpty()) return null
        return candidates[rnd.nextInt(candidates.size)]
    }
}
