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
import java.util.concurrent.ConcurrentHashMap

class RoomManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val rooms: ConcurrentHashMap<Int, Room> = ConcurrentHashMap()
    private val roomModels: ConcurrentHashMap<String, RoomModel> = ConcurrentHashMap()
    val customRoomModels: ConcurrentHashMap<Int, RoomModel> = ConcurrentHashMap()
    val roomTaskManager: RoomTaskManager = RoomTaskManager()

    fun load() {
        log.info("Loading rooms models...")
        log.info("Loading custom rooms models...")
        log.info("Loading rooms...")

        roomModels.clear()
        customRoomModels.clear()
        rooms.clear()

        RoomDao.getRoomModels().forEach { roomModels[it.id] = it }
        RoomDao.getCustomRoomModels().forEach { customRoomModels[it.roomId] = it }
        RoomDao.getRoomsData().forEach { roomData ->
            val model = if (roomData.modelName == "custom") customRoomModels[roomData.id] else roomModels[roomData.modelName]
            if (model != null) {
                rooms[roomData.id] = Room(roomData, model)
            } else {
                log.warn("Room {} has unknown model {}", roomData.id, roomData.modelName)
            }
        }

        log.info("Loaded {} room models!", roomModels.size)
        log.info("Loaded {} custom room models!", customRoomModels.size)
        log.info("Loaded {} rooms!", rooms.size)
    }

    fun createRoom(
        userId: Int,
        validSubscription: Boolean,
        name: String,
        description: String,
        model: String,
        category: Int,
        maxUsers: Int,
        tradeSettings: Int
    ): Room? {
        val roomModel = roomModels[model]

        if (name.length < 3 || roomModel == null || (roomModel.clubOnly && !validSubscription)) return null
        val roomId = RoomDao.createRoom(userId, name, description, model, category, maxUsers, tradeSettings)

        log.info("Created new room n° {} - name {}", roomId, name)
        val roomData = RoomData.createPrivate(
            id = roomId,
            userId = userId,
            name = name,
            description = description,
            model = model,
            category = category,
            maxUsers = maxUsers,
            tradeSettings = tradeSettings
        )
        val room = Room(roomData, roomModel)

        rooms[roomId] = room

        return room
    }

    /**
     * Busca os quartos ativos mais populares, opcionalmente por categoria.
     */
    fun getPopularRooms(categoryId: Int = -1, limit: Int = 40): List<Room> {
        return rooms.values
            .filter { it.running && it.userManager.entities.values.any { entity -> entity is RoomUser } }
            .filter { categoryId == -1 || it.roomData.category == categoryId }
            .sortedByDescending { it.userManager.entities.values.count { entity -> entity is RoomUser } }
            .take(limit)
    }

    /**
     * Motor de busca global (por nome, dono, descrição ou tags).
     */
    fun searchRooms(searchTerm: String, limit: Int = 50): List<Room> {
        val term = searchTerm.trim()
        if (term.isBlank()) return emptyList()

        return rooms.values.filter { room ->
            val data = room.roomData

            // Regra R63B: Quartos públicos não costumam aparecer na busca global
            if (data.roomType == RoomType.PUBLIC) return@filter false

            when {
                // Busca por Prefixo exato
                term.startsWith("owner:") ->
                    data.ownerName.equals(term.substring(6).trim(), ignoreCase = true)

                term.startsWith("tag:") ->
                    data.tags.any { it.equals(term.substring(4).trim(), ignoreCase = true) }

                term.startsWith("roomname:") ->
                    data.name.equals(term.substring(9).trim(), ignoreCase = true)

                // Busca por Mobi / Dynamic Category (navigator.roomsettings.allow_dynamic_categories)
                term.startsWith("furni:") -> {
                    if (!data.allowNavigatorDynamicCats) return@filter false
                    val furniQuery = term.substring(6).trim()
                    room.itemManager.items.values.any { item ->
                        item.furnishing.itemName.contains(furniQuery, ignoreCase = true)
                    }
                }

                // Busca Global sem recompilação repetitiva de Regex
                else ->
                    data.ownerName.contains(term, ignoreCase = true) ||
                            data.name.contains(term, ignoreCase = true) ||
                            data.description.contains(term, ignoreCase = true) ||
                            data.tags.any { it.contains(term, ignoreCase = true) }
            }
        }
            .sortedByDescending { it.userManager.entities.values.count { entity -> entity is RoomUser } }
            .take(limit)
    }

    /**
     * Retorna os quartos de um usuário específico.
     */
    fun getRoomsByOwner(ownerId: Int): List<Room> =
        rooms.values.filter { it.roomData.ownerId == ownerId }
}