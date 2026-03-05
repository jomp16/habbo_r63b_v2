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

package ovh.rwx.habbo.game.room.tasks

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerWalksOffFurni
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerWalksOnFurni
import ovh.rwx.habbo.game.room.IRoomTask
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import java.util.concurrent.atomic.AtomicInteger

class BanzaiTeleportTask(
    private val roomUser: RoomUser,
    private val sourceItem: RoomItem,
    private val targetItem: RoomItem
) : IRoomTask {
    private val counter = AtomicInteger(0)

    override fun executeTask(room: Room) {
        val currentCycle = counter.incrementAndGet()

        when (currentCycle) {
            1 -> {
                // Activate target teleport effect
                targetItem.extraData = "1"
                targetItem.update(updateDb = false, updateClient = true)

                // Trigger walks off on source
                room.itemManager.wiredHandler.triggerWired(WiredTriggerWalksOffFurni::class, roomUser, sourceItem)

                // Re-schedule for next cycle to teleport user
                room.roomTask?.addTask(room, this)
            }

            2 -> {
                // Teleport user instantly to target with random non-diagonal rotation
                val cardinalRotations = listOf(0, 2, 4, 6) // NORTH, EAST, SOUTH, WEST
                val randomRotation = cardinalRotations[(0 until cardinalRotations.size).random()]
                roomUser.stopWalking()
                roomUser.currentVector3 = targetItem.position
                roomUser.headRotation = randomRotation
                roomUser.bodyRotation = randomRotation
                roomUser.updateNeeded = true

                // Trigger walks on at target only if not a banzai teleport
                if (targetItem.furnishing.interactionType != InteractionType.BATTLE_BANZAI_TELEPORT) {
                    room.itemManager.wiredHandler.triggerWired(WiredTriggerWalksOnFurni::class, roomUser, targetItem)
                }

                // Reset target effect after 2 cycles
                targetItem.requestCycles(2)
            }
        }
    }
}