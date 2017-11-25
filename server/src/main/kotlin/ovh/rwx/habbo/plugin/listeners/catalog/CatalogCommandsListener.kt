/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.plugin.listeners.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.plugin.event.events.room.annotation.Command
import ovh.rwx.utils.plugin.api.PluginListener

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogCommandsListener : PluginListener() {
    @Command(["update_catalog", "reload_catalog"], permissionName = "cmd_update_catalogue")
    fun updateCatalogue(room: Room, roomUser: RoomUser, args: List<String>) {
        HabboServer.habboGame.catalogManager.load()

        HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated }.forEach {
            it.sendHabboResponse(Outgoing.CATALOG_UPDATE)
        }

        roomUser.habboSession!!.sendNotification("Catalog reloaded!")
    }

    @Command(["update_items", "reload_items"], permissionName = "cmd_update_items")
    fun updateItems(room: Room, roomUser: RoomUser, args: List<String>) {
        HabboServer.habboGame.itemManager.load()

        roomUser.habboSession!!.sendNotification("Items reloaded!")
    }
}