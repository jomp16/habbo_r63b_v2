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

package ovh.rwx.habbo.game.snowwar.objects

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.snowwar.SnowWarUser
import ovh.rwx.habbo.game.snowwar.enums.SnowWarActivityState
import ovh.rwx.habbo.game.snowwar.enums.SnowWarGameObjectType
import ovh.rwx.habbo.game.snowwar.utils.SnowWarMath
import kotlin.math.abs

class SnowWarMachine(
    objectId: Int,
    val x: Int,
    val y: Int,
    val direction: Int = 0,
    val maxSnowballs: Int = SnowWarMath.MACHINE_MAX_SNOWBALL_CAPACITY,
    val fuseObjectId: Int = 0
) : SnowWarGameObject(objectId, SnowWarGameObjectType.SNOWBALL_MACHINE) {

    var snowballCount: Int = 0
    var generatorTimer: Int = SnowWarMath.MACHINE_SNOWBALL_GENERATOR_TIME
    var reservedPickups: Int = 0

    override fun isAlive(): Boolean = true

    fun processGeneratorTick(): Boolean {
        if (generatorTimer > 0) {
            generatorTimer--
            return false
        }
        generatorTimer = SnowWarMath.MACHINE_SNOWBALL_GENERATOR_TIME
        return snowballCount < maxSnowballs
    }

    fun addSnowball() {
        if (snowballCount < maxSnowballs) {
            snowballCount++
        }
    }

    fun canPlayerPickup(user: SnowWarUser): Boolean {
        val dx = abs(user.currentTileX - x)
        val dy = abs(user.currentTileY - y)
        val isAdjacent = (dx <= 1 && dy <= 1 && !(dx == 0 && dy == 0)) ||
                (user.currentTileX in (x - 1..x + 2) && user.currentTileY in (y - 1..y + 2))
        if (!isAdjacent) return false
        if (user.isWalking) return false
        if (user.activityState != SnowWarActivityState.NORMAL && user.activityState != SnowWarActivityState.INVINCIBLE && user.activityState != SnowWarActivityState.MAKING_SNOWBALL) return false
        return snowballCount > reservedPickups && user.snowBallCount < SnowWarMath.MAX_SNOWBALLS
    }

    fun reservePickup() {
        reservedPickups++
    }

    fun transferReservedSnowballTo(user: SnowWarUser) {
        if (reservedPickups > 0) reservedPickups--
        if (snowballCount > 0 && user.snowBallCount < SnowWarMath.MAX_SNOWBALLS) {
            snowballCount--
            user.snowBallCount++
        }
    }

    fun testCollision(ball: SnowWarSnowball): Boolean {
        return ball.height < SnowWarMath.MACHINE_RADIUS && SnowWarMath.circlesOverlap(
            ball.locH,
            ball.locV,
            SnowWarMath.SNOWBALL_RADIUS,
            SnowWarMath.tileToWorld(x),
            SnowWarMath.tileToWorld(y),
            SnowWarMath.MACHINE_RADIUS
        )
    }

    override fun getChecksumVariables(): List<Int> {
        return listOf(
            type.id,
            objectId,
            SnowWarMath.tileToWorld(x),
            SnowWarMath.tileToWorld(y),
            direction,
            maxSnowballs,
            snowballCount,
            fuseObjectId
        )
    }

    override fun serialize(habboResponse: HabboResponse) {
        for (v in getChecksumVariables()) {
            habboResponse.writeInt(v)
        }
    }
}
