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
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.trigger.StateTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerStateChanged
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerStuffState
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomEntity

@Suppress("unused")
class OneWayGateItemInteractor : ItemInteractor() {
    override val interactionType = listOf(InteractionType.ONE_WAY_GATE)

    override fun onPlace(room: Room, roomEntity: RoomEntity?, roomItem: RoomItem) {
        super.onPlace(room, roomEntity, roomItem)

        roomItem.extraData = "0"
        roomItem.interactingUsers.clear()
    }

    override fun onRemove(room: Room, roomEntity: RoomEntity?, roomItem: RoomItem) {
        super.onRemove(room, roomEntity, roomItem)

        roomItem.extraData = "0"
        roomItem.interactingUsers.clear()
    }

    override fun processTick(room: Room, roomItem: RoomItem) {
        super.processTick(room, roomItem)

        if (roomItem.interactingUsers.containsKey(1)) {
            roomItem.interactingUsers.remove(1)?.let {
                it.walkingBlocked = false

                roomItem.extraData = "0"
                roomItem.update(updateDb = false, updateClient = true)
            }
        }
    }

    override fun onTrigger(room: Room, roomEntity: RoomEntity?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomEntity, roomItem, hasRights, request)

        if (roomEntity == null) return

        if (!roomItem.isTouching(roomEntity.currentVector3, roomEntity.bodyRotation, roomItem.position.z)) {
            roomEntity.moveTo(roomItem.getFrontPosition(), roomItem.getFrontRotation(), actingItem = roomItem)

            return
        }
        val behindVector2 = roomItem.getBehindPosition()

        if (room.roomGamemap.isBlocked(behindVector2)) return

        if (roomItem.interactingUsers.isEmpty()) {
            roomItem.interactingUsers[1] = roomEntity
            roomItem.extraData = "1"
            roomItem.update(updateDb = false, updateClient = true)

            roomEntity.walkingBlocked = true
            roomEntity.moveTo(roomItem.getBehindPosition(), ignoreBlocking = true)

            roomItem.requestTicks(3)
        }

        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerStateChanged::class, roomEntity,
            StateTriggerData(roomItem)
        )
        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerStuffState::class, roomEntity,
            StateTriggerData(roomItem)
        )
    }
}