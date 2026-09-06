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

package ovh.rwx.habbo.communication.outgoing.chest

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.chest.ChestEntry
import ovh.rwx.habbo.game.chest.serializeChestStorage

/**
 * Payload estruturado para envio de fragmento/chunk de itens do baú (CHEST_ITEMS_CHUNK).
 *
 * @param chestItemId ID do item do baú no quarto.
 * @param totalFragments Total de fragmentos da lista inteira.
 * @param fragmentNo Índice do fragmento atual (0-based).
 * @param entries Lista de itens contidos neste fragmento.
 */
data class ChestItemsChunkData(
    val chestItemId: Int,
    val totalFragments: Int,
    val fragmentNo: Int,
    val entries: List<ChestEntry>,
)

@Suppress("unused", "UNUSED_PARAMETER")
class ChestItemsChunkResponse {
    @Response(Outgoing.CHEST_ITEMS_CHUNK)
    fun response(habboResponse: HabboResponse, data: ChestItemsChunkData) {
        habboResponse.apply {
            writeInt(data.chestItemId)
            writeInt(data.totalFragments)
            writeInt(data.fragmentNo)
            writeInt(data.entries.size)

            data.entries.forEach { entry -> entry.serializeChestStorage(this) }
        }
    }
}
