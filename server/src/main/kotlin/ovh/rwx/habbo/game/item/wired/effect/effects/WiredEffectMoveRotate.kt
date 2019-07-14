/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredDelayEvent
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.effects.WiredEffectMoveRotate.RotationState.*
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.WiredDelayTask
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.util.Utils
import ovh.rwx.habbo.util.Vector2

@WiredItemInteractor(InteractionType.WIRED_EFFECT_MOVE_ROTATE)
class WiredEffectMoveRotate(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private val roomItemsIds: MutableList<Int> = mutableListOf()
    private var direction: DirectionState = DirectionState.NONE
    private var rotation: RotationState = NONE
    private var delay: Int = 0

    init {
        roomItem.wiredData?.let {
            roomItemsIds.addAll(it.items)
            direction = DirectionState.getDirectionState(it.options.getOrElse(0) { 0 })
            rotation = RotationState.getRotationState(it.options.getOrElse(1) { 0 })
            delay = it.delay
        }
    }

    override fun handle(roomUser: RoomUser?) {
        if (delay > 0) {
            room.roomTask?.addTask(room, WiredDelayTask(WiredDelayEvent(this, roomUser)))

            return
        }

        handleThing()
    }

    override fun handle(event: WiredDelayEvent) {
        super.handle(event)

        if (event.counter.incrementAndGet() >= delay) {
            event.finished = true

            handleThing()
        }
    }

    private fun handleThing() {
        roomItemsIds.forEach { itemId ->
            val roomItem = room.roomItems[itemId] ?: return@forEach

            val newVector2 = getVector2(roomItem.position.vector2)
            val newRotation = getRotation(roomItem.rotation)

            room.setFloorItem(roomItem, newVector2, newRotation, null, rollerId = -2)
        }
    }

    override fun setData(habboRequest: HabboRequest): Boolean {
        roomItem.wiredData?.let {
            roomItemsIds.clear()

            habboRequest.readInt() // useless?
            direction = DirectionState.getDirectionState(habboRequest.readInt())
            rotation = RotationState.getRotationState(habboRequest.readInt())

            habboRequest.readUTF()

            val amount = habboRequest.readInt()

            repeat(amount) {
                val itemId = habboRequest.readInt()

                if (room.roomItems.containsKey(itemId)) {
                    val roomItem1 = room.roomItems[itemId] ?: return@repeat

                    if (!roomItem1.furnishing.interactionType.name.startsWith("WIRED")) roomItemsIds += itemId
                }
            }


            if (delay < 0) delay = 0
            if (delay > 20) delay = 20

            it.delay = delay
            it.items = roomItemsIds.toList()
            it.options = listOf(direction.i, rotation.i)

            return true
        }

        return false
    }

    private fun getVector2(currentVector2: Vector2): Vector2 {
        return when (direction) {
            DirectionState.UP, DirectionState.DOWN, DirectionState.LEFT, DirectionState.RIGHT -> getVector2(currentVector2, direction)
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
            CLOCKWISE, COUNTER_CLOCKWISE -> getRotation(rotation1, rotation)
            RANDOM -> if (Utils.randInt(0..1) == 1) getRotation(rotation1, CLOCKWISE) else getRotation(rotation1, COUNTER_CLOCKWISE)
            else -> rotation1
        }
    }

    private fun getRotation(rotation1: Int, rotationState: RotationState): Int {
        var rotation = rotation1

        if (rotationState == CLOCKWISE) {
            rotation += 2
            if (rotation > 6) rotation = 0
        } else if (rotationState == COUNTER_CLOCKWISE) {
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
}