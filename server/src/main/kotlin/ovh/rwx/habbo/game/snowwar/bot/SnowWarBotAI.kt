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

import ovh.rwx.habbo.game.snowwar.*
import ovh.rwx.habbo.game.snowwar.bot.personality.SnowWarBotPersonalityFactory
import ovh.rwx.habbo.game.snowwar.enums.SnowWarActivityState
import ovh.rwx.habbo.game.snowwar.utils.SnowWarMath
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs
import kotlin.math.max

class SnowWarBotAI(private val game: SnowWarGame) {
    private val navigator = SnowWarBotNavigator(game)
    private val botNextActionTurn = ConcurrentHashMap<Int, Int>()

    fun processTurn(currentTurn: Int) {
        val rnd = ThreadLocalRandom.current()

        for (bot in game.users.values) {
            if (!bot.isBot || !bot.isAlive()) continue
            if (bot.activityState == SnowWarActivityState.STUNNED) continue
            if (bot.activityState == SnowWarActivityState.MAKING_SNOWBALL) continue

            val personality = SnowWarBotPersonalityFactory.forBotId(bot.objectId)

            // Defensive reaction: Dodge incoming enemy snowballs if stationary
            if (!bot.isWalking && bot.canMove()) {
                val incomingBall = game.snowballs.find { ball ->
                    ball.alive && ball.thrower.team != bot.team &&
                            SnowWarMath.distance(
                                ball.locH,
                                ball.locV,
                                bot.currentLocationX,
                                bot.currentLocationY
                            ) < 18_000
                }
                if (incomingBall != null && rnd.nextDouble() < 0.60) {
                    val dodgeTile = navigator.findDodgeTile(bot, incomingBall.direction)
                    if (dodgeTile != null) {
                        val sub = rnd.nextInt(3)
                        val targetX = SnowWarMath.tileToWorld(dodgeTile.first)
                        val targetY = SnowWarMath.tileToWorld(dodgeTile.second)
                        game.queueEvent(sub, NewMoveTargetGameEvent(bot.objectId, targetX, targetY))
                        botNextActionTurn[bot.objectId] = currentTurn + rnd.nextInt(4, 9)
                        continue
                    }
                }
            }

            val nextTurn = botNextActionTurn.getOrDefault(bot.objectId, 0)
            if (currentTurn < nextTurn) continue

            // Decision cooldown according to personality
            val cooldownTurns = personality.getDecisionCooldown(rnd)
            botNextActionTurn[bot.objectId] = currentTurn + cooldownTurns

            val hasSnowballs = bot.snowBallCount > 0
            val enemies = game.users.values.filter {
                it.team != bot.team && it.isAlive() && !it.isImmune() && it.hitPoints > 0
            }

            // Scenario 1: Snowball replenishment (seeking machines across map or making on ground)
            if (personality.shouldRestock(bot, rnd)) {
                val availableMachines = game.machines.filter { it.snowballCount > 0 }
                val availablePiles = game.piles.filter { it.snowballCount > 0 }
                val targetProviders = (availableMachines.map { it.x to it.y } + availablePiles.map { it.x to it.y })

                val targetCoords = if (targetProviders.isNotEmpty() && rnd.nextDouble() < 0.70) {
                    targetProviders.minByOrNull {
                        abs(bot.currentTileX - it.first) + abs(bot.currentTileY - it.second)
                    }
                } else null

                if (targetCoords != null && !navigator.isAdjacent(
                        bot.currentTileX,
                        bot.currentTileY,
                        targetCoords.first,
                        targetCoords.second
                    )
                ) {
                    val neighborTile = navigator.findWalkableAdjacentTile(targetCoords.first, targetCoords.second, bot)
                    if (neighborTile != null) {
                        val sub = rnd.nextInt(3)
                        val targetX = SnowWarMath.tileToWorld(neighborTile.first)
                        val targetY = SnowWarMath.tileToWorld(neighborTile.second)
                        game.queueEvent(sub, NewMoveTargetGameEvent(bot.objectId, targetX, targetY))
                        continue
                    }
                }

                // If no machine or already adjacent or random choice: make a snowball on ground
                if (bot.canMakeSnowballs()) {
                    val sub = rnd.nextInt(3)
                    game.queueEvent(sub, HumanStartsToMakeASnowballGameEvent(bot.objectId))
                    continue
                }
            }

            // Scenario 2: Attack enemies with snowballs (up to 85,000 units with trajectory selection)
            if (hasSnowballs && enemies.isNotEmpty()) {
                val inRangeEnemies = enemies.map { enemy ->
                    val dist = SnowWarMath.distance(
                        bot.currentLocationX,
                        bot.currentLocationY,
                        enemy.currentLocationX,
                        enemy.currentLocationY
                    )
                    enemy to dist
                }.filter { it.second <= 85_000 }

                if (inRangeEnemies.isNotEmpty() && bot.throwTimer <= 0 && rnd.nextDouble() < 0.75) {
                    // Smart target selection: 40% low HP finisher, 35% closest, 25% random
                    val roll = rnd.nextDouble()
                    val targetEntry = when {
                        roll < 0.40 -> inRangeEnemies.minByOrNull { it.first.hitPoints } ?: inRangeEnemies.first()
                        roll < 0.75 -> inRangeEnemies.minByOrNull { it.second } ?: inRangeEnemies.first()
                        else -> inRangeEnemies[rnd.nextInt(inRangeEnemies.size)]
                    }

                    val target = targetEntry.first
                    val dist = targetEntry.second
                    val trajectory = personality.selectTrajectory(dist, rnd)

                    // Spread / inaccuracy proportional to distance
                    val maxScatter = max(200, dist / 150)
                    val scatterX = rnd.nextInt(-maxScatter, maxScatter + 1)
                    val scatterY = rnd.nextInt(-maxScatter, maxScatter + 1)
                    val aimX = target.currentLocationX + scatterX
                    val aimY = target.currentLocationY + scatterY

                    val ballId = game.nextObjectId.getAndIncrement()
                    val sub = rnd.nextInt(3)

                    game.queueEvent(
                        sub,
                        HumanThrowsSnowballAtHumanGameEvent(
                            bot.objectId,
                            target.objectId,
                            trajectory
                        )
                    )
                    game.queueEvent(
                        sub,
                        CreateSnowballGameEvent(
                            ballId,
                            bot.objectId,
                            aimX,
                            aimY,
                            trajectory
                        )
                    )
                    continue
                }
            }

            // Scenario 3: Movement, Flanking, and Anti-Clustering
            if (!bot.isWalking && bot.canMove()) {
                val teammates = game.users.values.filter {
                    it.team == bot.team && it.objectId != bot.objectId && it.isAlive()
                }

                // Check for friendly crowding (within 2 tiles)
                val closeTeammate = teammates.find {
                    abs(bot.currentTileX - it.currentTileX) <= 2 && abs(bot.currentTileY - it.currentTileY) <= 2
                }

                val moveTile = if (closeTeammate != null && rnd.nextDouble() < 0.70) {
                    // Spread out away from teammate to avoid bunching up!
                    navigator.stepAway(
                        bot.currentTileX,
                        bot.currentTileY,
                        closeTeammate.currentTileX,
                        closeTeammate.currentTileY,
                        bot,
                        3
                    )
                } else if (enemies.isNotEmpty()) {
                    val closestEnemy = enemies.minByOrNull {
                        SnowWarMath.distance(
                            bot.currentLocationX,
                            bot.currentLocationY,
                            it.currentLocationX,
                            it.currentLocationY
                        )
                    }
                    val dist = if (closestEnemy != null) {
                        SnowWarMath.distance(
                            bot.currentLocationX,
                            bot.currentLocationY,
                            closestEnemy.currentLocationX,
                            closestEnemy.currentLocationY
                        )
                    } else 0

                    personality.decideMovement(bot, closestEnemy, dist, navigator, hasSnowballs, rnd)
                } else {
                    navigator.randomWalkableNeighbor(bot.currentTileX, bot.currentTileY, bot, 4)
                }

                if (moveTile != null) {
                    val sub = rnd.nextInt(3)
                    val targetX = SnowWarMath.tileToWorld(moveTile.first)
                    val targetY = SnowWarMath.tileToWorld(moveTile.second)
                    game.queueEvent(sub, NewMoveTargetGameEvent(bot.objectId, targetX, targetY))
                }
            }
        }
    }
}
