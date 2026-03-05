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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredFurniMove
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_FURNI_TO_FURNI)
class WiredEffectFurniToFurni(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    init {
        setData()
    }

    override fun code() = WiredEffectType.MOVE_FURNI_TO_FURNI.code
    override val requiresItems = true

    override fun onEffect(wiredContext: WiredContext) {
        // todo: fix this wired to use secondary furni source from advanced tab, when fixing wired item
        val items = wiredContext.getEffectiveFurnis(this)
        val targets = listOf(room.itemManager.floorItems[1377]!!)

        println("target=${this.roomItem.wiredData?.furniSources}")
        println("items=$items")
        println("targets=$targets")
        if (items.isEmpty()) return
        if (targets.isEmpty()) return

        val target = targets.random()
        val targetPos = target.position.vector2

        items.forEach { item ->
            val width = item.furnishing.width
            val length = item.furnishing.length

            val affectedTiles = HabboServer.habboGame.itemManager.getAffectedTiles(
                targetPos.x,
                targetPos.y,
                item.rotation,
                width,
                length
            )

            val freeTiles = affectedTiles.filter { tile ->
                !room.roomGamemap.isBlocked(tile, ignoreUsers = true) &&
                        !room.roomGamemap.cannotStackItem[tile.x][tile.y]
            }

            if (freeTiles.isNotEmpty()) {
                val newPos = freeTiles.random()
                val oldPos = item.position.copy() // Salva posição original

                if (room.itemManager.setFloorItem(item, newPos, item.rotation, null)) {
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
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
