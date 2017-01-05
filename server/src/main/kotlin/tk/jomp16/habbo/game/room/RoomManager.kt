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

package tk.jomp16.habbo.game.room

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.database.room.RoomDao
import tk.jomp16.habbo.game.room.model.RoomModel
import java.util.*

class RoomManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val rooms: MutableMap<Int, Room> = HashMap()
    val roomModels: MutableMap<String, RoomModel> = HashMap()
    val customRoomModels: MutableMap<Int, RoomModel> = HashMap()

    val roomTaskManager: RoomTaskManager = RoomTaskManager()

    init {
        log.info("Loading rooms models...")
        log.info("Loading custom rooms models...")
        log.info("Loading rooms...")

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

        val room = RoomDao.getRoomData(roomId).let { Room(it, roomModel) }

        rooms.put(roomId, room)

        return room
    }
}