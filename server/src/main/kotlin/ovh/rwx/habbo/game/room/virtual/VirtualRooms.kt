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

package ovh.rwx.habbo.game.room.virtual

import ovh.rwx.habbo.config.VirtualRoomsConfig
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomData
import ovh.rwx.habbo.game.room.model.RoomModel
import kotlin.random.Random

/**
 * Fábrica dos quartos virtuais de stress test (config `virtual_rooms`).
 *
 * Um quarto virtual é um [Room] comum (mesmo tick, pathfinding, broadcast e serialização) marcado com
 * [RoomData.virtual]: nunca lê nem grava no banco. Fica em `RoomManager.rooms`, então o navigator
 * (busca por nome/tag, populares, categorias) e a entrada por id funcionam como em qualquer sala.
 */
object VirtualRooms {
    /** Ids de sala virtuais: `ID_BASE + 1 .. ID_BASE + count`. Bem acima de qualquer id do banco. */
    const val ID_BASE = 2_100_000_000

    /** Dono fictício (nenhum usuário real tem este id; o ids dos bots começam em 2_000_000_000). */
    const val OWNER_ID = ID_BASE - 1
    const val OWNER_NAME = "StressTest"

    val TAGS = listOf("stress", "virtual")

    fun isVirtualRoomId(roomId: Int): Boolean = roomId > ID_BASE

    /**
     * Cria `config.count` quartos com modelos e categorias sorteados (semente fixa) e devolve-os
     * indexados por id. [models] só deve conter modelos livres (não-club); [categories] são ids de
     * categorias do navigator (vazio = categoria 0).
     */
    fun create(config: VirtualRoomsConfig, models: List<RoomModel>, categories: List<Int>): Map<Int, Room> {
        require(models.isNotEmpty()) { "virtual_rooms: nenhum modelo de sala disponível" }

        val random = Random(config.seed)
        val width = config.count.toString().length

        return (1..config.count).associate { index ->
            val model = models[random.nextInt(models.size)]
            val category = if (categories.isEmpty()) 0 else categories[random.nextInt(categories.size)]
            val id = ID_BASE + index

            val roomData = RoomData.createPrivate(
                id = id,
                userId = OWNER_ID,
                name = "${config.namePrefix} ${index.toString().padStart(width, '0')} (${model.id})",
                description = "Quarto virtual de stress test: existe só na memória, nada é salvo no banco.",
                model = model.id,
                category = category,
                maxUsers = config.maxUsers,
                tradeSettings = 0,
                tags = TAGS,
                virtual = true,
            )

            id to Room(roomData, model)
        }
    }
}
