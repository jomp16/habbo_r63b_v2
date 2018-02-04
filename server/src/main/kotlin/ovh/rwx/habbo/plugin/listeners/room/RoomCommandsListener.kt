/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.plugin.listeners.room

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.plugin.event.events.room.annotation.Command
import ovh.rwx.utils.plugin.api.PluginListener

@Suppress("unused", "UNUSED_PARAMETER")
class RoomCommandsListener : PluginListener() {
    @Command(["unload", "close"])
    fun closeRoom(room: Room, roomUser: RoomUser, args: List<String>) {
        if (!room.hasRights(roomUser.habboSession, true)) return

        HabboServer.habboGame.roomManager.roomTaskManager.removeRoomFromTask(room)
    }

    @Command(["coords"])
    fun coords(room: Room, roomUser: RoomUser, args: List<String>) {
        roomUser.habboSession?.sendNotification("X: ${roomUser.currentVector3.x}\n\nY: ${roomUser.currentVector3.y}\n\nZ: ${roomUser.currentVector3.z}\n\nHead rotation: ${roomUser.headRotation}\n\nBody rotation: ${roomUser.bodyRotation}")
    }

    @Command(["pickall"])
    fun pickall(room: Room, roomUser: RoomUser, args: List<String>) {
        if (room.hasRights(roomUser.habboSession, true)) {
            val roomItemsRemoved = room.roomItems.values.toList()
                    .filter { roomItem -> roomItem.furnishing.interactionType != InteractionType.POST_IT }
                    .filter { roomItem -> room.removeItem(roomUser, roomItem) }

            ItemDao.addRoomItemInventory(roomItemsRemoved)
        }
    }
}