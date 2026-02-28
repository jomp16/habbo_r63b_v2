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

import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.util.Direction
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_MOVE_ROTATE_USER)
class WiredEffectMoveRotateUser(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var moveDirection: Int = -1
    private var rotateDirection: Int = -1

    init {
        setData()
    }

    override fun code() = WiredEffectType.MOVE_USER.code
    override fun requiresItems() = false

    override fun setData() {
        roomItem.wiredData?.let {
            /*
             * moveDirection: -1=nenhum, 0-8=direções (usa Direction enum)
             * rotateDirection: -1=nenhum, 0-8=direções fixas, 9=90° direita, 10=90° esquerda
             */
            moveDirection = it.options.getOrElse(0) { -1 }
            rotateDirection = it.options.getOrElse(1) { -1 }
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        val targets = wiredContext.getEffectiveUsers(this)

        targets.forEach { user ->
            val offset = if (moveDirection == -1) Pair(0, 0) else Direction.fromCode(moveDirection).getOffset()
            val oldPos = user.currentVector3
            val newPos = Vector2(
                user.currentVector3.x + offset.first,
                user.currentVector3.y + offset.second
            )
            val newPosVector3 = Vector3(newPos, room.roomGamemap.getAbsoluteHeight(newPos))

            val newRotation = when (rotateDirection) {
                -1 -> user.bodyRotation
                in 0..8 -> rotateDirection
                9 -> Direction.fromCode(user.bodyRotation).turnRight90().code
                10 -> Direction.fromCode(user.bodyRotation).turnLeft90().code
                else -> user.bodyRotation
            }

            if (!room.roomGamemap.isBlocked(newPos, ignoreUsers = true)) {
                if (user.moveTo(newPos, newRotation, rollerId = -2, ignoreBlocking = true)) {
                    room.sendHabboResponse(Outgoing.ROOM_ROLLER, oldPos, newPosVector3, user.virtualID, roomItem.id, -1)
                    room.sendHabboResponse(
                        OutgoingR63A.ROOM_ROLLER,
                        oldPos,
                        newPosVector3,
                        user.virtualID,
                        roomItem.id,
                        -1
                    )
                }
            } else if (rotateDirection != -1) {
                user.bodyRotation = newRotation
                user.headRotation = newRotation
                user.updateNeeded = true
            }
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(-1, -1), "")
        }
    }
}
