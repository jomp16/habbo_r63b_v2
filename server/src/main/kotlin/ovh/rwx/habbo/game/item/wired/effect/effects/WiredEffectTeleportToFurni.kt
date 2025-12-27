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

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredDelayEvent
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.WiredDelayTask
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.room.user.RoomUserEffect
import ovh.rwx.habbo.util.Vector3

@WiredItemInteractor(InteractionType.WIRED_EFFECT_TELEPORT_TO)
class WiredEffectTeleportToFurni(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    init {
        setData()
    }

    override fun setData() {
        // No additional data to load
    }

    override fun handle(roomUser: RoomUser?) {
        val delay = roomItem.wiredData?.delay ?: 0
        if (delay > 0) {
            room.roomTask?.addTask(room, WiredDelayTask(WiredDelayEvent(this, roomUser)))
            return
        }
        handleThing(roomUser)
    }

    override fun handle(event: WiredDelayEvent) {
        super.handle(event)
        val delay = roomItem.wiredData?.delay ?: 0
        if (event.counter.incrementAndGet() >= delay) {
            event.finished = true
            handleThing(event.roomUser)
        }
    }

    private fun handleThing(roomUser: RoomUser?) {
        if (roomUser == null) return

        val roomItem = room.roomGamemap.getHighestItem(roomUser.currentVector3.vector2)
        val items = this.roomItem.wiredData?.items ?: return

        val roomItemId = if (roomItem != null) items.minus(roomItem.id).randomOrNull() else items.randomOrNull()
        if (roomItemId == null || !room.roomItems.containsKey(roomItemId)) return

        roomItem?.onUserWalksOff(roomUser, true)

        val roomItem1 = room.roomItems[roomItemId] ?: return

        val affectedTiles = roomItem1.affectedTiles

        affectedTiles.forEach { _ ->
            val tile = affectedTiles.random()

            if (!room.roomGamemap.isBlocked(tile)) {
                val newPosition = Vector3(tile, room.roomGamemap.getAbsoluteHeight(tile))

                roomUser.stopWalking()

                room.roomGamemap.updateRoomUserMovement(roomUser, roomUser.currentVector3.vector2, newPosition.vector2)

                roomUser.effect = RoomUserEffect(4, 5)
                roomUser.headRotation = roomItem1.rotation
                roomUser.bodyRotation = roomItem1.rotation
                roomUser.currentVector3 = newPosition
                roomUser.addUserStatuses(roomItem1)

                roomItem1.onUserWalksOn(roomUser, true)

                return@forEach
            }
        }
    }


    override fun writeDialog(habboResponse: HabboResponse, wiredData: WiredData) {
        habboResponse.apply {
            writeItems(wiredData)
            writeItemInfo(roomItem)
            writeEmptySettings()
            writeInt(WiredEffectType.TELEPORT.code)
            writeDelay(wiredData)
            writeBlockedTriggers(wiredData)
        }
    }
}