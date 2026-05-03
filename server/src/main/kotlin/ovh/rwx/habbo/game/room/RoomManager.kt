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

package ovh.rwx.habbo.game.room

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.game.room.user.RoomUser

class RoomManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val rooms: MutableMap<Int, Room> = mutableMapOf()
    private val roomModels: MutableMap<String, RoomModel> = mutableMapOf()
    val customRoomModels: MutableMap<Int, RoomModel> = mutableMapOf()
    val roomTaskManager: RoomTaskManager = RoomTaskManager()

    fun load() {
        log.info("Loading rooms models...")
        log.info("Loading custom rooms models...")
        log.info("Loading rooms...")

        roomModels.clear()
        customRoomModels.clear()
        rooms.clear()

        roomModels += RoomDao.getRoomModels().associateBy { it.id }
        customRoomModels += RoomDao.getCustomRoomModels().associateBy { it.roomId }
        rooms += RoomDao.getRoomsData().associateBy({ it.id }, {
            Room(it, if (it.modelName == "custom") customRoomModels[it.id]!! else roomModels[it.modelName]!!)
        })

        log.info("Loaded {} room models!", roomModels.size)
        log.info("Loaded {} custom room models!", customRoomModels.size)
        log.info("Loaded {} rooms!", rooms.size)
    }

    fun createRoom(userId: Int, validSubscription: Boolean, name: String, description: String, model: String, category: Int, maxUsers: Int, tradeSettings: Int): Room? {
        val roomModel = roomModels[model]

        if (name.length < 3 || roomModel == null || roomModel.clubOnly && !validSubscription) return null
        val roomId = RoomDao.createRoom(userId, name, description, model, category, maxUsers, tradeSettings)

        log.info("Created new room n° {} - name {}", roomId, name)
        val room = Room(RoomDao.getRoomData(roomId), roomModel)

        rooms[roomId] = room

        return room
    }

    /**
     * Busca os quartos ativos mais populares, opcionalmente por categoria.
     */
    fun getPopularRooms(categoryId: Int = -1, limit: Int = 40): List<Room> {
        return rooms.values
            .filter { it.running && it.userManager.entities.values.filterIsInstance<RoomUser>().isNotEmpty() }
            .filter { categoryId == -1 || it.roomData.category == categoryId }
            .sortedByDescending { it.userManager.entities.values.filterIsInstance<RoomUser>().size }
            .take(limit)
    }

    /**
     * Motor de busca global (por nome, dono, descrição ou tags).
     */
    fun searchRooms(searchTerm: String, limit: Int = 50): List<Room> {
        if (searchTerm.isBlank()) return emptyList()

        return rooms.values.filter { room ->
            val data = room.roomData

            // Regra R63B: Quartos públicos não costumam aparecer na busca global
            if (data.roomType == RoomType.PUBLIC) return@filter false

            when {
                // Busca por Prefixo exato
                searchTerm.startsWith("owner:") ->
                    data.ownerName.equals(searchTerm.substring(6), ignoreCase = true)

                searchTerm.startsWith("tag:") ->
                    data.tags.any { it.equals(searchTerm.substring(4), ignoreCase = true) }

                searchTerm.startsWith("roomname:") ->
                    data.name.equals(searchTerm.substring(9), ignoreCase = true)

                // Busca Global via Regex (Contém)
                else -> {
                    val regex = "(?i:.*${Regex.escape(searchTerm)}.*)".toRegex()
                    data.ownerName.matches(regex) ||
                            data.name.matches(regex) ||
                            data.description.matches(regex) ||
                            data.tags.any { it.matches(regex) }
                }
            }
        }
            .sortedByDescending { it.userManager.entities.values.filterIsInstance<RoomUser>().size }
            .take(limit)
    }

    /**
     * Retorna os quartos de um usuário específico.
     */
    fun getRoomsByOwner(ownerId: Int): List<Room> {
        return rooms.values.filter { it.roomData.ownerId == ownerId }
    }
}