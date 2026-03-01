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

package ovh.rwx.habbo.game.item.wired.effect.effects

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredFurniMove
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.util.Vector2
import kotlin.math.abs

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_FLEE_USER)
class WiredEffectFlee(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    init {
        setData()
    }

    override fun code() = WiredEffectType.FLEE.code
    override val requiresItems = true

    override fun onEffect(wiredContext: WiredContext) {
        val targets = wiredContext.getEffectiveFurnis(this)

        targets.forEach { item ->
            val closestUser = findClosestUserInLine(item)
            if (closestUser != null) {
                moveAwayFromUser(wiredContext, item, closestUser)
            }
        }
    }

    private fun findClosestUserInLine(item: RoomItem): RoomUser? {
        val itemPos = item.position.vector2
        var closestUser: RoomUser? = null
        var minDistance = Int.MAX_VALUE

        room.roomUsers.values.forEach { user ->
            val userPos = user.currentVector3.vector2
            if (userPos.x == itemPos.x || userPos.y == itemPos.y) {
                val distance = abs(userPos.x - itemPos.x) + abs(userPos.y - itemPos.y)
                if (distance < minDistance) {
                    minDistance = distance
                    closestUser = user
                }
            }
        }

        return closestUser
    }

    private fun moveAwayFromUser(wiredContext: WiredContext, item: RoomItem, user: RoomUser) {
        val itemPos = item.position.vector2
        val userPos = user.currentVector3.vector2

        val dx = itemPos.x - userPos.x
        val dy = itemPos.y - userPos.y

        val stepX = if (dx != 0) dx / Math.abs(dx.toDouble()) else 0.0
        val stepY = if (dy != 0) dy / Math.abs(dy.toDouble()) else 0.0

        val newPos = Vector2(
            (itemPos.x + stepX).toInt(),
            (itemPos.y + stepY).toInt()
        )

        if (!room.roomGamemap.isBlocked(newPos, ignoreUsers = true)) {
            val oldPos = item.position.copy() // Salva posição original

            if (room.setFloorItem(item, newPos, item.rotation, null)) {
                // Adiciona ao acumulador do ciclo
                wiredContext.batchedMovements.add(
                    WiredFurniMove(
                        furniId = item.id,
                        sourceX = oldPos.x,
                        sourceY = oldPos.y,
                        sourceZ = oldPos.z,
                        targetX = item.position.x,
                        targetY = item.position.y,
                        targetZ = item.position.z,
                        rotation = item.rotation
                    )
                )
            }
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
