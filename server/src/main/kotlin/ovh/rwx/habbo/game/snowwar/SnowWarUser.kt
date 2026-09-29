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
import ovh.rwx.habbo.game.snowwar.bot.SnowWarBotData
import ovh.rwx.habbo.game.snowwar.enums.SnowWarActivityState
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
    val session: HabboSession? = null,
    var team: SnowWarTeam = SnowWarTeam.BLUE,
    val botData: SnowWarBotData? = null
) : SnowWarGameObject(0, SnowWarGameObjectType.HUMAN), IHabboResponseSerialize {
    val isRealPlayer: Boolean get() = session != null
    val userId: Int get() = session?.userInformation?.id ?: (botData?.id ?: 0)
    val name: String get() = session?.userInformation?.username ?: (botData?.name ?: "")
    val figure: String get() = session?.userInformation?.figure ?: (botData?.figure ?: "")
    val gender: String get() = session?.userInformation?.gender ?: (botData?.gender ?: "M")
    val mission: String get() = session?.userInformation?.motto ?: (botData?.mission ?: "")

    val stats: SnowWarPlayerStats
        get() = if (session != null) {
            HabboServer.habboGame.snowWarManager.getPlayerStats(userId)
        } else {
            SnowWarPlayerStats(userId)
        }

    val skillLevel: Int get() = stats.skillLevel
    val totalScore: Int get() = stats.totalScore
    val scoreToNextLevel: Int get() = stats.scoreToNextLevel

    var currentLocationX: Int = 0
    var currentLocationY: Int = 0
    var currentTileX: Int = 0
    var currentTileY: Int = 0
    var bodyDirection: Int = 2
    var hitPoints: Int = 5
    var snowBallCount: Int = 0
    val isBot: Boolean get() = session == null
    var activityTimer: Int = 0
    var activityState: SnowWarActivityState = SnowWarActivityState.NORMAL
    var nextTileX: Int = -1
    var nextTileY: Int = -1
    var moveTargetX: Int = 0
    var moveTargetY: Int = 0
    var pickupTimer: Int = SnowWarMath.MACHINE_PICKUP_INTERVAL
    var throwTimer: Int = 0 // AS3 _SafeStr_8227: SNOWBALL_THROW_INTERVAL=5; prevents burst throws

    val score = AtomicInteger(0)
    val hits = AtomicInteger(0)
    val kills = AtomicInteger(0)
    val throws = AtomicInteger(0)
    val deaths = AtomicInteger(0)
    val treeHits = AtomicInteger(0)

    var isLoaded: Boolean = session == null

    val isWalking: Boolean
        get() = (currentLocationX != moveTargetX || currentLocationY != moveTargetY) &&
                (activityState == SnowWarActivityState.NORMAL || activityState == SnowWarActivityState.INVINCIBLE)

    override fun isAlive(): Boolean = true

    fun canMove(): Boolean =
        activityState == SnowWarActivityState.NORMAL || activityState == SnowWarActivityState.INVINCIBLE

    fun canMakeSnowballs(): Boolean =
        canMove() && snowBallCount < SnowWarMath.MAX_SNOWBALLS

    fun changeMoveTarget(newTargetX: Int, newTargetY: Int) {
        // AS3 HumanGameObject.changeMoveTarget:
        // if (_SafeStr_8225 == 1) { _SafeStr_8225 = 0; _SafeStr_8224 = 0; }
        // if (_SafeStr_8225 == 0 || _SafeStr_8225 == 3) { moveTarget.change2DLocation(x, y); }
        if (activityState == SnowWarActivityState.MAKING_SNOWBALL) {
            activityState = SnowWarActivityState.NORMAL
            activityTimer = 0
        }
        if (canMove()) {
            moveTargetX = newTargetX
            moveTargetY = newTargetY
        }
    }

    fun startMakingSnowball() {
        // AS3 HumanGameObject.startMakingSnowball:
        // if (canMakeSnowballs()) { _SafeStr_8225 = 1; _SafeStr_8224 = 20; stopMovement(); }
        if (canMakeSnowballs()) {
            activityState = SnowWarActivityState.MAKING_SNOWBALL
            activityTimer = SnowWarMath.CREATING_TIMER
            stopWalking()
        }
    }

    fun throwSnowball(targetX: Int, targetY: Int): Boolean {
        // AS3 HumanGameObject.throwSnowball:
        if (snowBallCount < 1) return false
        stopWalking()
        val angle360 = SnowWarMath.getAngleFromComponents(targetX - currentLocationX, targetY - currentLocationY)
        bodyDirection = SnowWarMath.direction360To8(angle360)
        snowBallCount--
        throwTimer = SnowWarMath.SNOWBALL_THROW_INTERVAL
        throws.incrementAndGet()
        return true
    }

    fun isImmune(): Boolean =
        activityState == SnowWarActivityState.STUNNED || activityState == SnowWarActivityState.INVINCIBLE

    fun setupSpawn(
        index: Int,
        spawnTile: Pair<Int, Int>,
        arenaWidth: Int = 50,
        arenaHeight: Int = 50
    ) {
        val startTileX = spawnTile.first
        val startTileY = spawnTile.second

        val startWorldX = SnowWarMath.tileToWorld(startTileX)
        val startWorldY = SnowWarMath.tileToWorld(startTileY)

        val centerWorldX = SnowWarMath.tileToWorld(arenaWidth / 2)
        val centerWorldY = SnowWarMath.tileToWorld(arenaHeight / 2)

        val angle360 = SnowWarMath.getAngleFromComponents(
            centerWorldX - startWorldX,
            centerWorldY - startWorldY
        )
        val bodyDir = SnowWarMath.direction360To8(angle360)

        val field = SnowWarGameObject::class.java.getDeclaredField("objectId")
        field.isAccessible = true
        field.setInt(this, index + 1)

        currentLocationX = startWorldX
        currentLocationY = startWorldY
        currentTileX = startTileX
        currentTileY = startTileY
        bodyDirection = bodyDir
        hitPoints = SnowWarMath.INITIAL_HEALTH
        snowBallCount = SnowWarMath.MAX_SNOWBALLS
        activityTimer = 0
        activityState = SnowWarActivityState.NORMAL
        nextTileX = -1
        nextTileY = -1
        moveTargetX = currentLocationX
        moveTargetY = currentLocationY
        pickupTimer = SnowWarMath.MACHINE_PICKUP_INTERVAL
        throwTimer = 0
    }

    fun testCollision(ball: SnowWarSnowball): Boolean {
        return !(ball.thrower == this || isImmune() || activityState == SnowWarActivityState.STUNNED || ball.height >= SnowWarMath.AVATAR_COLLISION_HEIGHT) && SnowWarMath.circlesOverlap(
            ball.locH,
            ball.locV,
            SnowWarMath.SNOWBALL_RADIUS,
            currentLocationX,
            currentLocationY,
            SnowWarMath.AVATAR_RADIUS
        )
    }

    fun tickSnowballPickupTimer(): Boolean {
        if (isWalking || (activityState != SnowWarActivityState.NORMAL && activityState != SnowWarActivityState.INVINCIBLE)) {
            pickupTimer = SnowWarMath.MACHINE_PICKUP_INTERVAL
            return false
        }
        if (pickupTimer > 0) {
            pickupTimer--
            return false
        }
        pickupTimer = SnowWarMath.MACHINE_PICKUP_INTERVAL
        return true
    }

    fun onActivityTimerExpired() {
        when (activityState) {
            SnowWarActivityState.MAKING_SNOWBALL -> {
                activityState = SnowWarActivityState.NORMAL
                snowBallCount = min(snowBallCount + 1, SnowWarMath.MAX_SNOWBALLS)
            }

            SnowWarActivityState.STUNNED -> {
                activityState = SnowWarActivityState.INVINCIBLE
                activityTimer = SnowWarMath.INVINCIBILITY_TIMER
                hitPoints = SnowWarMath.INITIAL_HEALTH
            }

            SnowWarActivityState.INVINCIBLE -> {
                activityState = SnowWarActivityState.NORMAL
            }

            else -> {}
        }
    }

    fun subturn(isTileWalkable: (tileX: Int, tileY: Int) -> Boolean) {
        if (activityTimer > 0) {
            if (activityTimer == 1) {
                onActivityTimerExpired()
            }
            activityTimer--
        }

        // AS3 _SafeStr_8227: throw cooldown, decremented unconditionally every subturn
        if (throwTimer > 0) throwTimer--

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

                    // AS3 lines 234600-234618
                    if (!isTileWalkable(candidateX, candidateY)) {
                        // AS3: if (moveTarget.equals(candidateTile.location)) { stopMovement(); return; }
                        val candidateWorldX = SnowWarMath.tileToWorld(candidateX)
                        val candidateWorldY = SnowWarMath.tileToWorld(candidateY)
                        if (moveTargetX == candidateWorldX && moveTargetY == candidateWorldY) {
                            nextTileX = -1
                            nextTileY = -1
                            stopWalking()
                            return
                        }

                        val leftDir = (resolvedDir - 1 + 8) % 8
                        val leftX = currentTileX + offsetX[leftDir]
                        val leftY = currentTileY + offsetY[leftDir]
                        if (isTileWalkable(leftX, leftY)) {
                            resolvedDir = leftDir
                            candidateX = leftX
                            candidateY = leftY
                        } else {
                            val rightDir =
                                (leftDir + 2) % 8 // rotateDirection(2) from leftDir == rotateDirection(+1) from primary
                            val rightX = currentTileX + offsetX[rightDir]
                            val rightY = currentTileY + offsetY[rightDir]
                            if (isTileWalkable(rightX, rightY)) {
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
        // AS3 stopMovement(): snaps location and moveTarget to the current/next tile
        if (nextTileX != -1) {
            currentTileX = nextTileX
            currentTileY = nextTileY
            nextTileX = -1
            nextTileY = -1
        }
        currentLocationX = SnowWarMath.tileToWorld(currentTileX)
        currentLocationY = SnowWarMath.tileToWorld(currentTileY)
        moveTargetX = currentLocationX
        moveTargetY = currentLocationY
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
            writeInt(0) // Variable 9 is _SafeStr_8223 in AS3 HumanGameObject (uninitialized, always 0)
            writeInt(activityTimer)
            writeInt(activityState.id)
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
            0, // Variable 9 is _SafeStr_8223 in AS3 HumanGameObject (uninitialized, always 0)
            activityTimer,
            activityState.id,
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
