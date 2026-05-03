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
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomEntity

@Suppress("unused")
class HabboWheelItemInteractor : ItemInteractor() {
    override val interactionType = listOf(InteractionType.HABBO_WHEEL)

    override fun onPlace(room: Room, roomEntity: RoomEntity?, roomItem: RoomItem) {
        super.onPlace(room, roomEntity, roomItem)

        roomItem.extraData = "1"
    }

    override fun onRemove(room: Room, roomEntity: RoomEntity?, roomItem: RoomItem) {
        super.onRemove(room, roomEntity, roomItem)

        roomItem.extraData = "-1"
    }

    override fun onTrigger(room: Room, roomEntity: RoomEntity?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomEntity, roomItem, hasRights, request)

        if (!hasRights) return

        if (roomItem.extraData != "-1") {
            roomItem.extraData = "-1"
            roomItem.update(updateDb = false, updateClient = true)
            roomItem.requestTicks(6)
        }

        if (roomEntity != null) room.itemManager.wiredHandler.triggerWired(
            WiredTriggerStateChanged::class,
            roomEntity,
            StateTriggerData(roomItem)
        )
    }

    override fun processTick(room: Room, roomItem: RoomItem) {
        super.processTick(room, roomItem)

        roomItem.extraData = (1..10).random().toString()

        roomItem.update(updateDb = true, updateClient = true)
    }
}