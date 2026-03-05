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
import ovh.rwx.habbo.util.Vector3

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_SET_ALTITUDE)
class WiredEffectSetAltitude(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var operator: Operator = Operator.SET
    private var altitude: Double = 0.0

    init {
        setData()
    }

    override fun code() = WiredEffectType.SET_FURNI_ALTITUDE.code
    override val requiresItems = true

    override fun setData() {
        roomItem.wiredData?.let {
            altitude = it.options.getOrElse(0) { 0 } / 100.0
            operator = Operator.fromCode(it.options.getOrElse(1) { 2 })
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        val targets = wiredContext.getEffectiveFurnis(this)
        targets.forEach { item ->
            val currentTiles = item.affectedTiles

            currentTiles.forEach { vector2 ->
                room.roomGamemap.getUsersFromVector2(vector2).forEach { roomUser ->
                    item.onUserWalksOff(roomUser, true)
                    roomUser.removeUserStatuses()
                    roomUser.currentVector3 = Vector3(vector2, room.roomGamemap.getAbsoluteHeight(vector2))
                    roomUser.updateNeeded = true
                }
            }

            val newAltitude = when (operator) {
                Operator.INCREASE -> item.position.z + altitude
                Operator.DECREASE -> item.position.z - altitude
                Operator.SET -> altitude
            }

            val oldPos = item.position.copy() // Salva posição original

            if (room.itemManager.setFloorItem(item, oldPos.vector2, item.rotation, null, newAltitude)) {
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

    /**
     * Operador para modificar altitude
     */
    private enum class Operator(val code: Int) {
        /** Aumentar altitude */
        INCREASE(0),

        /** Diminuir altitude */
        DECREASE(1),

        /** Configurar valor exato */
        SET(2);

        companion object {
            fun fromCode(code: Int) = entries.find { it.code == code } ?: SET
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0, 2), "")
        }
    }
}
