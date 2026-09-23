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

package ovh.rwx.habbo.game.item.interactors

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemInteractor
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.trigger.StateTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerStateChanged
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerStuffState
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomUser
import kotlin.math.abs

@Suppress("unused")
class DefaultItemInteractor : ItemInteractor() {
    override val interactionType = listOf(InteractionType.DEFAULT)

    override fun onTrigger(room: Room, roomEntity: RoomEntity?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomEntity, roomItem, hasRights, request)

        if (!hasRights) return

        if (roomEntity != null && roomItem.furnishing.type == ItemType.FLOOR) {
            val userPos = roomEntity.currentVector3.vector2
            val isNear = roomItem.affectedTiles.any { tile ->
                abs(tile.x - userPos.x) <= 1 && abs(tile.y - userPos.y) <= 1
            }
            if (!isNear) {
                roomEntity.moveTo(roomItem.position.vector2, actingItem = roomItem)
                return
            }
        }

        val modes = roomItem.furnishing.interactionModesCount - 1
        var currentMode = roomItem.extraData.toIntOrNull() ?: 0
        if (modes <= 0) return
        if (++currentMode > modes) currentMode = 0

        roomItem.extraData = currentMode.toString()

        if (roomItem.furnishing.stackMultiple) {
            room.itemManager.setFloorItem(
                roomItem,
                roomItem.position.vector2,
                roomItem.rotation,
                roomEntity as? RoomUser,
                roomItem.totalHeight
            )
        }

        roomItem.update(updateDb = true, updateClient = true)

        if (roomEntity != null) {
            room.itemManager.wiredHandler.triggerWired(
                WiredTriggerStateChanged::class,
                roomEntity,
                StateTriggerData(roomItem)
            )
        }
        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerStuffState::class,
            roomEntity,
            StateTriggerData(roomItem)
        )
    }
}