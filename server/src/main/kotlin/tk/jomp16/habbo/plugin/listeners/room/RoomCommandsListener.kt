/*
 * Copyright (C) 2017 jomp16
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

package tk.jomp16.habbo.plugin.listeners.room

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser
import tk.jomp16.habbo.plugin.event.events.room.annotation.Command
import tk.jomp16.utils.plugin.api.PluginListener

class RoomCommandsListener : PluginListener() {
    // todo: pickall

    @Command(arrayOf("unload", "close"))
    fun closeRoom(room: Room, roomUser: RoomUser, args: List<String>) {
        if (!roomUser.room.hasRights(roomUser.habboSession, true)) return

        HabboServer.habboGame.roomManager.roomTaskManager.removeRoomFromTask(roomUser.room)
    }

    @Command(arrayOf("coords"))
    fun coords(room: Room, roomUser: RoomUser, args: List<String>) {
        roomUser.habboSession?.sendNotification("X: ${roomUser.currentVector3.x}\n\nY: ${roomUser.currentVector3.y}\n\nZ: ${roomUser.currentVector3.z}\n\nHead rotation: ${roomUser.headRotation}\n\nBody rotation: ${roomUser.bodyRotation}")
    }
}