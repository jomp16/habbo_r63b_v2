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

package ovh.rwx.habbo.game.item.interactors

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemInteractor
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerStateChanged
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
class DefaultItemInteractor : ItemInteractor() {
    override val interactionType = listOf(InteractionType.DEFAULT)

    override fun onTrigger(room: Room, roomUser: RoomUser?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomUser, roomItem, hasRights, request)

        if (!hasRights) return
        val modes = roomItem.furnishing.interactionModesCount - 1
        var currentMode = if (roomItem.extraData.isEmpty()) 0 else roomItem.extraData.toInt()

        if (modes == 0) return
        // todo: check if this is working properly
        if (++currentMode > modes) currentMode = 0

        roomItem.extraData = currentMode.toString()

        if (roomItem.furnishing.stackMultiple) {
            // todo: check if this is working properly
            room.setFloorItem(roomItem, roomItem.position.vector2, roomItem.rotation, roomUser, roomItem.totalHeight)
        }

        roomItem.update(updateDb = true, updateClient = true)

        if (roomUser != null) room.wiredHandler.triggerWired(WiredTriggerStateChanged::class, roomUser, roomItem)
    }
}