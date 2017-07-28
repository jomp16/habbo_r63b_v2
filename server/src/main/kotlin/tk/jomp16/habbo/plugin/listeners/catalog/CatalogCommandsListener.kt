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

package tk.jomp16.habbo.plugin.listeners.catalog

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser
import tk.jomp16.habbo.plugin.event.events.room.annotation.Command
import tk.jomp16.utils.plugin.api.PluginListener

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogCommandsListener : PluginListener() {
    @Command(arrayOf("update_catalog", "reload_catalog"), permissionName = "cmd_update_catalogue")
    fun updateCatalogue(room: Room, roomUser: RoomUser, args: List<String>) {
        HabboServer.habboGame.catalogManager.load()

        HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated }.forEach {
            it.sendHabboResponse(Outgoing.CATALOG_UPDATE)
        }

        roomUser.habboSession!!.sendNotification("Catalog reloaded!")
    }

    @Command(arrayOf("update_items", "reload_items"), permissionName = "cmd_update_items")
    fun updateItems(room: Room, roomUser: RoomUser, args: List<String>) {
        HabboServer.habboGame.itemManager.load()

        roomUser.habboSession!!.sendNotification("Items reloaded!")
    }
}