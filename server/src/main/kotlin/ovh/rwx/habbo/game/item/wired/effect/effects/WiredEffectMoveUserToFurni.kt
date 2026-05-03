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
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUserEffect
import ovh.rwx.habbo.util.Vector3

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_USER_TO_FURNI)
class WiredEffectMoveUserToFurni(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var walkMode: WalkMode = WalkMode.STOP_IF_NOT_TARGET

    init {
        setData()
    }

    override fun code() = WiredEffectType.MOVE_USER_TO_FURNI.code
    override val requiresItems = true

    override fun setData() {
        roomItem.wiredData?.let {
            walkMode = WalkMode.fromCode(it.options.getOrElse(0) { 0 })
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        val targets = wiredContext.getEffectiveUsers(this)
        val furnis = wiredContext.getEffectiveFurnis(this)

        if (targets.isEmpty() || furnis.isEmpty()) return

        val targetUser = targets.random()
        val targetFurni = furnis.random()

        val affectedTiles = targetFurni.affectedTiles
        val freeTiles = affectedTiles.filter { tile ->
            !room.roomGamemap.isBlocked(tile, ignoreUsers = true)
        }

        if (freeTiles.isNotEmpty()) {
            val newPos = freeTiles.random()
            val position = Vector3(newPos, room.roomGamemap.getAbsoluteHeight(newPos))
            val wasWalking = targetUser.walking

            when (walkMode) {
                WalkMode.STOP_IF_NOT_TARGET -> if (wasWalking && newPos != targetUser.objectiveVector2) targetUser.stopWalking()
                WalkMode.KEEP_WALKING -> {} // Continua andando
                WalkMode.STOP -> targetUser.stopWalking()
            }

            room.roomGamemap.updateRoomEntityMovement(
                targetUser,
                targetUser.currentVector3.vector2,
                newPos
            )

            targetUser.effect = RoomUserEffect(4, 5)
            targetUser.headRotation = targetFurni.rotation
            targetUser.bodyRotation = targetFurni.rotation
            targetUser.currentVector3 = position
            targetUser.addEntityStatuses(targetFurni)

            targetFurni.onEntityWalksOn(targetUser, true)
        }
    }

    /**
     * Comportamento de caminhada quando o usuário é movido
     */
    private enum class WalkMode(val code: Int) {
        /** Continuar andando se for movido para junto do alvo */
        STOP_IF_NOT_TARGET(0),

        /** Continuar andando */
        KEEP_WALKING(1),

        /** Parar de andar */
        STOP(2);

        companion object {
            fun fromCode(code: Int) = entries.find { it.code == code } ?: STOP_IF_NOT_TARGET
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0), "")
        }
    }
}
