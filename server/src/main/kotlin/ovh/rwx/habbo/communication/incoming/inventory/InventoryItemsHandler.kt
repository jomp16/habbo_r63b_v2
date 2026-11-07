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

package ovh.rwx.habbo.communication.incoming.inventory

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class InventoryItemsHandler {
    @Handler(Incoming.INVENTORY_ITEMS)
    @HandlerR63A(IncomingR63A.INVENTORY_ITEMS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val items = habboSession.habboInventory.items.values

        // De 2011-02-25 até 2013-05-22 é enviado floor e wall separados, cada um com a sua categoria
        if (habboSession.isVersionAtLeast(2011, 2, 25) && habboSession.isVersionBefore(2013, 5, 22)) {
            habboSession.sendResponse(Outgoing.INVENTORY_ITEMS, OutgoingR63A.INVENTORY_ITEMS, "S", items.filter { it.furnishing.type == ItemType.FLOOR })
            habboSession.sendResponse(Outgoing.INVENTORY_ITEMS, OutgoingR63A.INVENTORY_ITEMS, "I", items.filter { it.furnishing.type == ItemType.WALL })
        } else {
            habboSession.sendResponse(Outgoing.INVENTORY_ITEMS, OutgoingR63A.INVENTORY_ITEMS, "", items)
        }
    }
}
