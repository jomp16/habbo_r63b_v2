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

package ovh.rwx.habbo.communication.outgoing.inventory

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.user.UserItem

@Suppress("unused", "UNUSED_PARAMETER")
class InventoryItemsResponse {
    // type: categoria do lote (S = floor, I = wall); só trafega de 2011-02-25 até 2013-05-22
    @Response(Outgoing.INVENTORY_ITEMS)
    @ResponseR63A(OutgoingR63A.INVENTORY_ITEMS)
    fun response(habboResponse: HabboResponse, type: String, items: Collection<UserItem>) {
        habboResponse.apply {
            // Na build 201102252317, os fragments entraram oficialmente no cliente!
            if (isVersionAtLeast(2011, 2, 25)) {
                if (isVersionBefore(2013, 5, 22)) writeUTF(type)
                writeInt(1) // totalFragments
                writeInt(0) // fragmentNo
            }

            writeInt(items.size)

            items.forEach { serialize(it) }
        }
    }
}
