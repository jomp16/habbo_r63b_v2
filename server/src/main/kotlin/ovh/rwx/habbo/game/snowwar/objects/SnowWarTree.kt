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
import ovh.rwx.habbo.game.snowwar.enums.SnowWarGameObjectType
import ovh.rwx.habbo.game.snowwar.utils.SnowWarMath

class SnowWarTree(
    objectId: Int,
    val x: Int,
    val y: Int,
    val direction: Int = 0,
    val height: Int = SnowWarMath.TREE_COLLISION_HEIGHT,
    val fuseObjectId: Int = 0,
    val maxHits: Int = SnowWarMath.TREE_MAX_HITS
) : SnowWarGameObject(objectId, SnowWarGameObjectType.TREE) {

    var hits: Int = 0

    override fun isAlive(): Boolean = true

    fun hit(): Int {
        if (hits < maxHits) {
            hits++
        }
        return hits
    }

    fun testCollision(ball: SnowWarSnowball): Boolean {
        // AS3 TreeGameObject: collisionHeight returns _SafeStr_5674 (height = 3200)
        return hits < maxHits &&
                ball.height < height &&
                SnowWarMath.circlesOverlap(
                    ball.locH,
                    ball.locV,
                    SnowWarMath.SNOWBALL_RADIUS,
                    SnowWarMath.tileToWorld(x),
                    SnowWarMath.tileToWorld(y),
                    SnowWarMath.TREE_RADIUS
                )
    }

    override fun getChecksumVariables(): List<Int> {
        return listOf(
            type.id,
            objectId,
            SnowWarMath.tileToWorld(x),
            SnowWarMath.tileToWorld(y),
            direction,
            height,
            fuseObjectId,
            maxHits,
            hits
        )
    }

    override fun serialize(habboResponse: HabboResponse) {
        for (v in getChecksumVariables()) {
            habboResponse.writeInt(v)
        }
    }
}
