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
import ovh.rwx.habbo.game.snowwar.enums.SnowWarGameObjectType
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTrajectory
import ovh.rwx.habbo.game.snowwar.utils.SnowWarMath
import kotlin.math.min

class SnowWarSnowball(
    objectId: Int,
    val thrower: SnowWarUser,
    startWorldX: Int,
    startWorldY: Int,
    targetWorldX: Int,
    targetWorldY: Int,
    trajectoryRequested: SnowWarTrajectory
) : SnowWarGameObject(objectId, SnowWarGameObjectType.SNOWBALL) {

    var locH: Int = startWorldX
    var locV: Int = startWorldY
    var height: Int = 3000

    val direction: Int
    var timeToLive: Int
    val parabolaOffset: Int
    val planarVelocity: Int
    val trajectory: Int

    var alive: Boolean = true

    init {
        val flightPath = SnowWarMath.calculateFlightPathWorld(
            startWorldX,
            startWorldY,
            targetWorldX,
            targetWorldY,
            trajectoryRequested
        )
        direction = flightPath[0]
        timeToLive = flightPath[1]
        parabolaOffset = flightPath[2]
        planarVelocity = flightPath[3]
        trajectory = flightPath[4]
    }

    override fun isAlive(): Boolean = alive

    fun kill() {
        alive = false
    }

    private fun calculateHeight(ttl: Int): Int {
        val distanceFromPeak = ttl - parabolaOffset
        val trajectoryEnum = SnowWarTrajectory.fromId(trajectory)
        val heightMultiplier = when (trajectoryEnum) {
            SnowWarTrajectory.QUICK -> 10
            SnowWarTrajectory.SHORT_LOB -> 25
            else -> 50
        }

        val calculated =
            3000 + heightMultiplier * ((parabolaOffset * parabolaOffset) - (distanceFromPeak * distanceFromPeak))
        return if (trajectoryEnum == SnowWarTrajectory.QUICK) min(calculated, 3000) else calculated
    }

    fun calculateFrameMovement() {
        if (!alive) return
        timeToLive--

        val deltaH = (SnowWarMath.getBaseVelX(direction) * planarVelocity) / SnowWarMath.VELOCITY_DIVISOR
        val deltaV = (SnowWarMath.getBaseVelY(direction) * planarVelocity) / SnowWarMath.VELOCITY_DIVISOR

        locH += deltaH
        locV += deltaV
        height = calculateHeight(timeToLive)
    }

    fun hasFloorCollision(heightMapRows: List<String>): Boolean {
        // AS3 testCollisionWithGround:
        // if (location3D.z < 1) return true;
        // var tile = getTileAt(tileX, tileY);
        // if (tile) return location3D.z < tile.height;
        // return false; (holes 'x' have no tile, so flying balls pass over them)
        if (height < 1) return true
        val tileX = SnowWarMath.worldToTile(locH)
        val tileY = SnowWarMath.worldToTile(locV)

        if (tileY !in heightMapRows.indices || tileX !in heightMapRows[tileY].indices) {
            return false
        }

        val ch = heightMapRows[tileY][tileX]
        if (ch == 'x' || ch == 'X') return false
        val tileHeight = if (ch.isDigit()) (ch - '0') * 3200 else (10 + (ch - 'a')) * 3200
        return height < tileHeight
    }

    override fun getChecksumVariables(): List<Int> {
        return listOf(
            type.id,
            objectId,
            locH,
            locV,
            height,
            direction,
            trajectory,
            timeToLive,
            thrower.objectId,
            parabolaOffset,
            planarVelocity
        )
    }

    override fun serialize(habboResponse: HabboResponse) {
        for (v in getChecksumVariables()) {
            habboResponse.writeInt(v)
        }
    }
}
