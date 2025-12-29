/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.util.Utils
import ovh.rwx.habbo.util.Vector2

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_MOVE_ROTATE)
class WiredEffectMoveRotate(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var direction: DirectionState = DirectionState.NONE
    private var rotation: RotationState = RotationState.NONE

    init {
        setData()
    }

    override fun code() = WiredEffectType.MOVE_FURNI.code

    override fun setData() {
        roomItem.wiredData?.let {
            direction = DirectionState.getDirectionState(it.options.getOrElse(0) { 0 })
            rotation = RotationState.getRotationState(it.options.getOrElse(1) { 0 })
        }
    }

    override fun onEffect(roomUser: RoomUser?) {
        roomItem.wiredData?.items?.forEach { itemId ->
            val roomItem = room.roomItems[itemId] ?: return@forEach

            val newVector2 = getVector2(roomItem.position.vector2)
            val newRotation = getRotation(roomItem.rotation)

            room.setFloorItem(roomItem, newVector2, newRotation, null, rollerId = -2)
        }
    }


    private fun getVector2(currentVector2: Vector2): Vector2 {
        return when (direction) {
            DirectionState.UP, DirectionState.DOWN, DirectionState.LEFT, DirectionState.RIGHT -> getVector2(
                currentVector2,
                direction
            )

            DirectionState.LEFT_RIGHT -> if (Utils.randInt(0..1) == 1) {
                getVector2(currentVector2, DirectionState.LEFT)
            } else {
                getVector2(currentVector2, DirectionState.RIGHT)
            }

            DirectionState.UP_DOWN -> if (Utils.randInt(0..1) == 1) {
                getVector2(currentVector2, DirectionState.UP)
            } else {
                getVector2(currentVector2, DirectionState.DOWN)
            }

            DirectionState.RANDOM -> when (Utils.randInt(1..4)) {
                1 -> getVector2(currentVector2, DirectionState.UP)
                2 -> getVector2(currentVector2, DirectionState.DOWN)
                3 -> getVector2(currentVector2, DirectionState.LEFT)
                4 -> getVector2(currentVector2, DirectionState.RIGHT)
                else -> currentVector2
            }

            else -> currentVector2
        }
    }

    private fun getVector2(currentVector2: Vector2, directionState: DirectionState): Vector2 {
        return when (directionState) {
            DirectionState.UP -> Vector2(currentVector2.x, currentVector2.y - 1)
            DirectionState.DOWN -> Vector2(currentVector2.x, currentVector2.y + 1)
            DirectionState.LEFT -> Vector2(currentVector2.x - 1, currentVector2.y)
            DirectionState.RIGHT -> Vector2(currentVector2.x + 1, currentVector2.y)
            else -> currentVector2
        }
    }

    private fun getRotation(rotation1: Int): Int {
        return when (rotation) {
            RotationState.CLOCKWISE, RotationState.COUNTER_CLOCKWISE -> getRotation(rotation1, rotation)
            RotationState.RANDOM -> if (Utils.randInt(0..1) == 1) getRotation(
                rotation1,
                RotationState.CLOCKWISE
            ) else getRotation(
                rotation1,
                RotationState.COUNTER_CLOCKWISE
            )

            else -> rotation1
        }
    }

    private fun getRotation(rotation1: Int, rotationState: RotationState): Int {
        var rotation = rotation1

        if (rotationState == RotationState.CLOCKWISE) {
            rotation += 2
            if (rotation > 6) rotation = 0
        } else if (rotationState == RotationState.COUNTER_CLOCKWISE) {
            rotation -= 2
            if (rotation < 0) rotation = 6
        }

        return rotation
    }

    private enum class DirectionState(val i: Int) {
        NONE(0),
        RANDOM(1),
        LEFT_RIGHT(2),
        UP_DOWN(3),
        UP(4),
        RIGHT(5),
        DOWN(6),
        LEFT(7);

        companion object {
            fun getDirectionState(i: Int) = values().firstOrNull { it.i == i } ?: NONE
        }
    }

    private enum class RotationState(val i: Int) {
        NONE(0),
        CLOCKWISE(1),
        COUNTER_CLOCKWISE(2),
        RANDOM(3);

        companion object {
            fun getRotationState(i: Int) = values().firstOrNull { it.i == i } ?: NONE
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0, 0), "")
        }
    }
}