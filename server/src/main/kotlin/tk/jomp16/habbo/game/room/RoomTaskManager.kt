/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.game.room

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import java.util.*
import java.util.concurrent.TimeUnit

class RoomTaskManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val scheduledFutureMap: MutableList<RoomTask> = ArrayList()
    val rooms: MutableList<Room> = ArrayList()

    fun addRoomToTask(room: Room) {
        if (rooms.contains(room)) return

        rooms += room

        val roomTask: RoomTask

        val tmpTasks = scheduledFutureMap.filter { it.rooms.size < HabboServer.habboConfig.roomTaskConfig.maxRoomPerThread }

        if (tmpTasks.isNotEmpty()) {
            roomTask = tmpTasks[0]
        } else {
            roomTask = RoomTask()

            execute(roomTask)
        }

        roomTask.addRoom(room)
    }

    private fun execute(roomTask: RoomTask) {
        // dis is a timeout task manager
        /*val handler = HabboServer.executor.submit(roomTask)

        HabboServer.executor.schedule({
            if (handler.isDone) {
                execute(roomTask)
            } else {
                // todo: throw users outta here
                handler.cancel(true)
            }
        }, HabboServer.habboConfig.roomTaskConfig.delayMilliseconds.toLong(), TimeUnit.MILLISECONDS)*/

        HabboServer.executor.scheduleAtFixedRate({
            try {
                roomTask.run()
            } catch (e: Exception) {
                log.error("An exception happened!", e)
            }
        }, 0, HabboServer.habboConfig.roomTaskConfig.delayMilliseconds.toLong(), TimeUnit.MILLISECONDS)
    }

    fun removeRoomFromTask(room: Room) {
        if (!rooms.contains(room)) return

        rooms -= room

        scheduledFutureMap.filter { it.rooms.contains(room) }.forEach { it.removeRoom(room) }
    }
}