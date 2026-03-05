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

package ovh.rwx.habbo.game.item.interactors.banzai

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemInteractor
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.BanzaiTeleportTask
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
class BanzaiTeleportItemInteractor : ItemInteractor() {
    override val interactionType = listOf(InteractionType.BATTLE_BANZAI_TELEPORT)

    override fun onPlace(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        super.onPlace(room, roomUser, roomItem)
        roomItem.extraData = "0"
    }

    override fun onCycle(room: Room, roomItem: RoomItem) {
        super.onCycle(room, roomItem)

        if (roomItem.extraData == "1") {
            roomItem.extraData = "0"
            roomItem.update(updateDb = false, updateClient = true)
        }
    }

    override fun onUserWalksOn(room: Room, roomUser: RoomUser, roomItem: RoomItem) {
        super.onUserWalksOn(room, roomUser, roomItem)

        // Find all other banzai teleports in the room
        val teleports = room.itemManager.floorItems.values.filter {
            it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TELEPORT && it.id != roomItem.id
        }

        if (teleports.isEmpty()) return

        // Select random target teleport
        val targetTeleport = teleports[(0 until teleports.size).random()]

        // Activate source teleport effect for 2 cycles
        roomItem.extraData = "1"
        roomItem.update(updateDb = false, updateClient = true)
        roomItem.requestCycles(2)

        // Start teleportation task after 1 cycle
        room.roomTask?.addTask(room, BanzaiTeleportTask(roomUser, roomItem, targetTeleport))
    }
}