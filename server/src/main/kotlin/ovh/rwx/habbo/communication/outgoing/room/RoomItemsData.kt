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

package ovh.rwx.habbo.communication.outgoing.room

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.item.room.RoomItem

/**
 * Itens de um quarto (chão ou parede) e seus donos, como as mensagens ROOM_FLOOR_ITEMS e ROOM_WALL_ITEMS os enviam.
 *
 * @param items Itens do quarto.
 */
data class RoomItemsData(val items: Collection<RoomItem>) {
    /**
     * Donos distintos dos itens (id -> nome), na ordem em que aparecem. Os itens do Builders Club não entram: o
     * serialize deles escreve um id fixo, sem dono.
     */
    val owners: Map<Int, String> by lazy {
        items.filterNot { it.buildersClub }
            .map { it.userId }
            .distinct()
            .associateWith { UserInformationDao.getUserInformationById(it)?.username ?: "null" }
    }
}

/**
 * Escreve os itens do quarto: desde 21/09/2011 (RELEASE63-36096) a lista de donos (id, nome) vem antes deles; cada item
 * traz o id do seu dono.
 */
fun HabboResponse.writeRoomItems(data: RoomItemsData) {
    if (isVersionAtLeast(2011, 9, 21)) {
        writeInt(data.owners.size)
        data.owners.forEach { (id, name) ->
            writeInt(id)
            writeUTF(name)
        }
    }
    writeInt(data.items.size)
    data.items.forEach { serialize(it) }
}
