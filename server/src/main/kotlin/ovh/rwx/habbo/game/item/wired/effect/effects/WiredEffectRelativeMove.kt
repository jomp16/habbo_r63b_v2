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
import ovh.rwx.habbo.util.Vector2

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_RELATIVE_MOVE)
class WiredEffectRelativeMove(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var x: Int = 0
    private var y: Int = 0

    init {
        setData()
    }

    override fun code() = WiredEffectType.RELATIVE_FURNI_MOVE.code
    override val requiresItems = true

    override fun setData() {
        roomItem.wiredData?.let {
            x = it.options.getOrElse(0) { 0 }
            y = it.options.getOrElse(1) { 0 }
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        val targets = wiredContext.getEffectiveFurnis(this)

        targets.forEach { item ->
            val newPos = Vector2(
                item.position.x + x,
                item.position.y + y
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
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0, 0), "")
        }
    }
}
