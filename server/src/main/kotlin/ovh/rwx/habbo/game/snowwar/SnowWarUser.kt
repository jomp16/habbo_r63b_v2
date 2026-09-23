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

package ovh.rwx.habbo.game.snowwar

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.game.snowwar.enums.SnowWarGameObjectType
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTeam
import ovh.rwx.habbo.game.snowwar.enums.SnowWarUserSerializeMode
import ovh.rwx.habbo.game.snowwar.objects.SnowWarGameObject
import ovh.rwx.habbo.game.snowwar.objects.SnowWarSnowball
import ovh.rwx.habbo.game.snowwar.utils.SnowWarMath
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

class SnowWarUser(
    val session: HabboSession,
    var team: SnowWarTeam = SnowWarTeam.BLUE
) : SnowWarGameObject(0, SnowWarGameObjectType.HUMAN), IHabboResponseSerialize {
    val userId: Int get() = session.userInformation.id
    val name: String get() = session.userInformation.username
    val figure: String get() = session.userInformation.figure
    val gender: String get() = session.userInformation.gender
    val mission: String get() = session.userInformation.motto

    val stats: SnowWarPlayerStats
        get() = HabboServer.habboGame.snowWarManager.getPlayerStats(userId)

    val skillLevel: Int get() = stats.skillLevel
    val totalScore: Int get() = stats.totalScore
    val scoreToNextLevel: Int get() = stats.scoreToNextLevel

    var currentLocationX: Int = 0
    var currentLocationY: Int = 0
    var currentTileX: Int = 0
    var currentTileY: Int = 0
    var bodyDirection: Int = 2
    var hitPoints: Int = 5
    var pendingHealth: Int = 5
    var pendingStun: Boolean = false
    var snowBallCount: Int = 0
    var isBot: Int = 0
    var activityTimer: Int = 0
    var activityState: Int = 0 // 0=NORMAL, 1=MAKING_SNOWBALL, 2=STUNNED, 3=INVINCIBLE
    var nextTileX: Int = -1
    var nextTileY: Int = -1
    var moveTargetX: Int = 0
    var moveTargetY: Int = 0
    var pickupTimer: Int = SnowWarMath.CREATING_TIMER

    val score = AtomicInteger(0)
    val hits = AtomicInteger(0)
    val kills = AtomicInteger(0)
    val throws = AtomicInteger(0)
    val deaths = AtomicInteger(0)

    var isLoaded: Boolean = false

    val isWalking: Boolean
        get() = (currentLocationX != moveTargetX || currentLocationY != moveTargetY) && (activityState == 0 || activityState == 3)

    override fun isAlive(): Boolean = true

    fun canMove(): Boolean = activityState == 0 || activityState == 3 // NORMAL or INVINCIBLE

    fun isImmune(): Boolean = activityState == 2 || activityState == 3 // STUNNED or INVINCIBLE

    fun setupSpawn(index: Int, arena: SnowWarArenaData? = null) {
        val isTeam1 = team == SnowWarTeam.BLUE
        val spawns = if (arena != null) {
            if (isTeam1) arena.blueSpawns else arena.redSpawns
        } else {
            emptyList()
        }
        val spawn = if (spawns.isNotEmpty()) {
            spawns[index % spawns.size]
        } else {
            if (isTeam1) 22 to 9 else 30 to 43
        }

        val startTileX = spawn.first
        val startTileY = spawn.second
        val bodyDir = if (isTeam1) 2 else 6

        val field = SnowWarGameObject::class.java.getDeclaredField("objectId")
        field.isAccessible = true
        field.setInt(this, index + 1)

        currentLocationX = SnowWarMath.tileToWorld(startTileX)
        currentLocationY = SnowWarMath.tileToWorld(startTileY)
        currentTileX = startTileX
        currentTileY = startTileY
        bodyDirection = bodyDir
        hitPoints = SnowWarMath.INITIAL_HEALTH
        pendingHealth = SnowWarMath.INITIAL_HEALTH
        pendingStun = false
        snowBallCount = SnowWarMath.MAX_SNOWBALLS
        isBot = 0
        activityTimer = 0
        activityState = 0
        nextTileX = -1
        nextTileY = -1
        moveTargetX = currentLocationX
        moveTargetY = currentLocationY
        pickupTimer = SnowWarMath.CREATING_TIMER
    }

    fun testCollision(ball: SnowWarSnowball): Boolean {
        return !(ball.thrower == this || isImmune() || pendingStun || ball.height >= SnowWarMath.AVATAR_COLLISION_HEIGHT) && SnowWarMath.circlesOverlap(
            ball.locH,
            ball.locV,
            SnowWarMath.SNOWBALL_RADIUS,
            currentLocationX,
            currentLocationY,
            SnowWarMath.AVATAR_RADIUS
        )
    }

    fun tickSnowballPickupTimer(): Boolean {
        if (isWalking || (activityState != 0 && activityState != 3)) {
            pickupTimer = SnowWarMath.CREATING_TIMER
            return false
        }
        if (pickupTimer > 0) {
            pickupTimer--
            return false
        }
        pickupTimer = SnowWarMath.CREATING_TIMER
        return true
    }

    fun resetSnowballPickupTimer() {
        pickupTimer = SnowWarMath.CREATING_TIMER
    }

    fun onActivityTimerExpired() {
        when (activityState) {
            1 -> { // MAKING_SNOWBALL
                activityState = 0
                snowBallCount = min(snowBallCount + 1, SnowWarMath.MAX_SNOWBALLS)
            }

            2 -> { // STUNNED
                activityState = 3 // INVINCIBLE
                activityTimer = SnowWarMath.INVINCIBILITY_TIMER
                hitPoints = SnowWarMath.INITIAL_HEALTH
                pendingHealth = SnowWarMath.INITIAL_HEALTH
                pendingStun = false
            }

            3 -> { // INVINCIBLE
                activityState = 0
            }
        }
    }

    fun subturn(heightmapRows: List<String>) {
        if (activityTimer > 0) {
            if (activityTimer == 1) {
                onActivityTimerExpired()
            }
            activityTimer--
        }

        if (canMove()) {
            if (nextTileX != -1 && nextTileY != -1) {
                moveTowardsNextTile()
            } else {
                val currentTileWorldX = SnowWarMath.tileToWorld(currentTileX)
                val currentTileWorldY = SnowWarMath.tileToWorld(currentTileY)
                // AS3: !currentTile.locationIsInTileRange(moveTarget)
                val deltaX = kotlin.math.abs(currentTileWorldX - moveTargetX)
                val deltaY = kotlin.math.abs(currentTileWorldY - moveTargetY)
                if (deltaX >= SnowWarMath.TILE_HALFWIDTH || deltaY >= SnowWarMath.TILE_HALFWIDTH) {
                    val angle360 = SnowWarMath.getAngleFromComponents(
                        moveTargetX - currentTileWorldX,
                        moveTargetY - currentTileWorldY
                    )
                    val dir8 = SnowWarMath.direction360To8(angle360)

                    val offsetX = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
                    val offsetY = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)

                    var resolvedDir = dir8
                    var candidateX = currentTileX + offsetX[resolvedDir]
                    var candidateY = currentTileY + offsetY[resolvedDir]

                    // AS3: if primary direction blocked, try rotateDirection(-1), then rotateDirection(+2)
                    if (!isTileWalkable(candidateX, candidateY, heightmapRows)) {
                        val leftDir = (resolvedDir - 1 + 8) % 8
                        val leftX = currentTileX + offsetX[leftDir]
                        val leftY = currentTileY + offsetY[leftDir]
                        if (isTileWalkable(leftX, leftY, heightmapRows)) {
                            resolvedDir = leftDir
                            candidateX = leftX
                            candidateY = leftY
                        } else {
                            val rightDir =
                                (leftDir + 2) % 8 // rotateDirection(2) from leftDir == rotateDirection(+1) from primary
                            val rightX = currentTileX + offsetX[rightDir]
                            val rightY = currentTileY + offsetY[rightDir]
                            if (isTileWalkable(rightX, rightY, heightmapRows)) {
                                resolvedDir = rightDir
                                candidateX = rightX
                                candidateY = rightY
                            } else {
                                // All directions blocked — stay put
                                return
                            }
                        }
                    }

                    bodyDirection = resolvedDir
                    nextTileX = candidateX
                    nextTileY = candidateY
                    moveTowardsNextTile()
                }
            }
        }
    }

    private fun isTileWalkable(tileX: Int, tileY: Int, heightmapRows: List<String>): Boolean {
        if (tileY < 0 || tileY >= heightmapRows.size) return false
        val row = heightmapRows[tileY]
        return !(tileX < 0 || tileX >= row.length) && row[tileX] != 'x' && row[tileX] != 'X'
    }

    private fun moveTowardsNextTile() {
        val targetWorldX = SnowWarMath.tileToWorld(nextTileX)
        val targetWorldY = SnowWarMath.tileToWorld(nextTileY)

        var curX = currentLocationX
        val diffX = curX - targetWorldX
        if (diffX != 0) {
            if (diffX < 0) {
                curX = if (diffX > -SnowWarMath.SUBTURN_MOVEMENT) targetWorldX else curX + SnowWarMath.SUBTURN_MOVEMENT
            } else {
                curX = if (diffX < SnowWarMath.SUBTURN_MOVEMENT) targetWorldX else curX - SnowWarMath.SUBTURN_MOVEMENT
            }
        }

        var curY = currentLocationY
        val diffY = curY - targetWorldY
        if (diffY != 0) {
            if (diffY < 0) {
                curY = if (diffY > -SnowWarMath.SUBTURN_MOVEMENT) targetWorldY else curY + SnowWarMath.SUBTURN_MOVEMENT
            } else {
                curY = if (diffY < SnowWarMath.SUBTURN_MOVEMENT) targetWorldY else curY - SnowWarMath.SUBTURN_MOVEMENT
            }
        }

        currentLocationX = curX
        currentLocationY = curY

        // AS3: _SafeStr_8220.distanceTo(_SafeStr_8219.location) < (534 / 2)
        // No AS3, Location3D.distanceTo faz distância Manhattan: abs(dx) + abs(dy) + abs(dz)
        val manhattanDistance = kotlin.math.abs(curX - targetWorldX) + kotlin.math.abs(curY - targetWorldY)

        if (manhattanDistance < (SnowWarMath.SUBTURN_MOVEMENT / 2)) {
            currentTileX = nextTileX
            currentTileY = nextTileY
            nextTileX = -1
            nextTileY = -1
        }
    }

    fun stopWalking() {
        moveTargetX = currentLocationX
        moveTargetY = currentLocationY
        nextTileX = -1
        nextTileY = -1
    }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        val mode = if (params.isNotEmpty() && params[0] is SnowWarUserSerializeMode) {
            params[0] as SnowWarUserSerializeMode
        } else {
            SnowWarUserSerializeMode.LOBBY
        }

        when (mode) {
            SnowWarUserSerializeMode.LOBBY -> serializeLobby(habboResponse)
            SnowWarUserSerializeMode.ARENA -> serializeArena(habboResponse)
            SnowWarUserSerializeMode.STAGE_HUMAN -> serializeStageHuman(habboResponse)
            SnowWarUserSerializeMode.TEAM_ENDING -> serializeTeamEnding(habboResponse)
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        serializeHabboResponse(habboResponse, *params)
    }

    fun serializeLobby(habboResponse: HabboResponse) {
        habboResponse.apply {
            writeInt(userId)
            writeUTF(name)
            writeUTF(figure)
            writeUTF(gender)
            writeInt(team.id)
            writeInt(skillLevel)
            writeInt(totalScore)
            writeInt(scoreToNextLevel)
        }
    }

    fun serializeArena(habboResponse: HabboResponse) {
        habboResponse.apply {
            writeInt(userId)
            writeUTF(name)
            writeUTF(figure)
            writeUTF(gender)
            writeInt(team.id)
        }
    }

    fun serializeStageHuman(habboResponse: HabboResponse) {
        habboResponse.apply {
            writeInt(SnowWarGameObjectType.HUMAN.id)
            writeInt(objectId)
            writeInt(currentLocationX)
            writeInt(currentLocationY)
            writeInt(currentTileX)
            writeInt(currentTileY)
            writeInt(bodyDirection)
            writeInt(hitPoints)
            writeInt(snowBallCount)
            writeInt(isBot)
            writeInt(activityTimer)
            writeInt(activityState)
            writeInt(if (nextTileX != -1) nextTileX else currentTileX)
            writeInt(if (nextTileY != -1) nextTileY else currentTileY)
            writeInt(moveTargetX)
            writeInt(moveTargetY)
            writeInt(score.get())
            writeInt(team.id)
            writeInt(userId)
            writeUTF(name)
            writeUTF(mission)
            writeUTF(figure)
            writeUTF(gender)
        }
    }

    override fun serialize(habboResponse: HabboResponse) {
        serializeStageHuman(habboResponse)
    }

    override fun getChecksumVariables(): List<Int> {
        return listOf(
            SnowWarGameObjectType.HUMAN.id,
            objectId,
            currentLocationX,
            currentLocationY,
            currentTileX,
            currentTileY,
            bodyDirection,
            hitPoints,
            snowBallCount,
            isBot,
            activityTimer,
            activityState,
            if (nextTileX != -1) nextTileX else currentTileX,
            if (nextTileY != -1) nextTileY else currentTileY,
            moveTargetX,
            moveTargetY,
            score.get(),
            team.id,
            userId
        )
    }

    fun serializeTeamEnding(habboResponse: HabboResponse) {
        habboResponse.apply {
            writeUTF(name)
            writeInt(userId)
            writeUTF(figure)
            writeUTF(gender)
            writeInt(score.get())
            // Game2PlayerStatsData
            writeInt(score.get())
            writeInt(kills.get())
            writeInt(deaths.get())
            writeInt(hits.get())
            writeInt(deaths.get())
            writeInt(throws.get())
            writeInt(throws.get())
            writeInt(0) // snowballsFromMachine
            writeInt(0) // friendlyHits
            writeInt(0) // friendlyKills
        }
    }
}
