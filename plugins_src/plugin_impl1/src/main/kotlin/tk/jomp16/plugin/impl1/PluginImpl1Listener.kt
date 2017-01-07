/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.plugin.impl1

import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser
import tk.jomp16.habbo.plugin.event.events.room.annotation.Command
import tk.jomp16.utils.plugin.api.PluginListener

@Suppress("unused", "UNUSED_PARAMETER")
class PluginImpl1Listener : PluginListener() {
    override fun onCreate() = log.trace("Olá mundo de ${javaClass.simpleName}!")

    override fun onDestroy() = log.trace("Tchau mundo de ${javaClass.simpleName}!")

    @Command(arrayOf("hello_world1"))
    fun handleHelloWorld1(room: Room, roomUser: RoomUser, args: List<String>) {
        roomUser.habboSession?.sendNotification("Hello World 1!")
    }

    @Command(arrayOf("hello_world2"), rank = 7)
    fun handleHelloWorld2(room: Room, roomUser: RoomUser, args: List<String>) {
        roomUser.habboSession?.sendNotification("Hello World 2 with rank 7!")
    }

    @Command(arrayOf("hello_world3"), rank = 7, permissionName = "acc_mod_tools")
    fun handleHelloWorld3(room: Room, roomUser: RoomUser, args: List<String>) {
        roomUser.habboSession?.sendNotification("Hello World 2 with rank 7 and acc_mod_tools!")
    }

    @Command(arrayOf("hello_world4"), rank = Int.MAX_VALUE, permissionName = "acc_mod_tools")
    fun handleHelloWorld4(room: Room, roomUser: RoomUser, args: List<String>) {
        roomUser.habboSession?.sendNotification("Hello World 2 with rank ${Int.MAX_VALUE} and acc_mod_tools!")
    }

    @Command(arrayOf("hello_world5"), permissionName = "acc_mod_tools")
    fun handleHelloWorld5(room: Room, roomUser: RoomUser, args: List<String>) {
        roomUser.habboSession?.sendNotification("Hello World 2 with acc_mod_tools!")
    }
}