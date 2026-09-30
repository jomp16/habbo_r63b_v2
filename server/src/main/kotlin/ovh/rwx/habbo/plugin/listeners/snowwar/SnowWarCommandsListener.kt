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

package ovh.rwx.habbo.plugin.listeners.snowwar

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.plugin.api.PluginListener
import ovh.rwx.habbo.plugin.event.events.room.annotation.Command

@Suppress("unused", "UNUSED_PARAMETER")
class SnowWarCommandsListener : PluginListener() {
    @Command(["arena", "setarena", "snowwar_arena", "snowwar"], rank = 7)
    fun arenaCommand(room: Room, roomUser: RoomUser, args: List<String>) {
        val arg = if (args.size > 1) args.subList(1, args.size).joinToString(" ") else args.getOrElse(0) { "" }
        val result = HabboServer.serverConsole.handleArenaCommand(arg)
        roomUser.habboSession.sendNotification(result)
    }
}
