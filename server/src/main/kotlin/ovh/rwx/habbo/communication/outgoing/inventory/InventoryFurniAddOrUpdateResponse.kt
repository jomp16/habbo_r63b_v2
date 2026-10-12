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
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.item.user.UserItem

@Suppress("unused", "UNUSED_PARAMETER")
class InventoryFurniAddOrUpdateResponse {
    // FurniListAddOrUpdateParser no AS3:
    //  - até WIN63-202504011439: `var _local_2:int = 1` (hardcoded, SEM count no wire, 1 item por pacote)
    //  - de WIN63-202504171200 em diante: `var _local_2:int = _arg_1.readInteger()` (count no wire)
    // Um count extra no wire antigo faz o readString ler o itemId como length UTF → "Failed to parse".
    @Response(Outgoing.INVENTORY_FURNI_ADD_OR_UPDATE)
    fun response(habboResponse: HabboResponse, items: Collection<UserItem>) {
        habboResponse.apply {
            if (isVersionAtLeast(2025, 4, 17)) {
                writeInt(items.size)
                items.forEach { serialize(it) }
            } else {
                check(items.size == 1) { "INVENTORY_FURNI_ADD_OR_UPDATE expects exactly 1 item, got ${items.size}" }
                serialize(items.first())
            }
        }
    }
}
