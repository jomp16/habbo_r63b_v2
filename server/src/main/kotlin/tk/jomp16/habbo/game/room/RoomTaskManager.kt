/*
 * Copyright (C) 2016 jomp16
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

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.kotlin.random
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class RoomTaskManager {
    val scheduledFutureMap: MutableMap<RoomTask, ScheduledFuture<*>> = HashMap()
    val rooms: MutableList<Room> = mutableListOf()

    fun addRoomToTask(room: Room) {
        if (rooms.contains(room)) return

        rooms += room

        val tmpTasks = scheduledFutureMap.keys.filter { it.rooms.size < HabboServer.habboConfig.roomTaskConfig.maxRoomPerThread }

        val roomTask = if (tmpTasks.isNotEmpty()) tmpTasks.random() else RoomTask()

        if (!scheduledFutureMap.containsKey(roomTask)) scheduledFutureMap.put(roomTask, HabboServer.serverScheduledExecutor.scheduleAtFixedRate(roomTask, 0, HabboServer.habboConfig.roomTaskConfig.delayMilliseconds.toLong(), TimeUnit.MILLISECONDS))

        roomTask.addRoom(room)
    }

    fun removeRoomFromTask(room: Room) {
        if (!rooms.contains(room)) return

        rooms -= room

        scheduledFutureMap.keys.filter { it.rooms.contains(room) }.forEach { it.removeRoom(room) }
    }
}