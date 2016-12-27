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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.room.RoomDao
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

class RoomTask : Runnable {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val rooms: MutableSet<Room> = CopyOnWriteArraySet()
    val queuedTasks: MutableMap<Room, Queue<IRoomTask>> = ConcurrentHashMap()

    fun addRoom(room: Room) {
        log.info("Loading room n° {} - name {}", room.roomData.id, room.roomData.caption)

        rooms += room
        queuedTasks.put(room, ArrayDeque())

        room.emptyCounter.set(0)
        room.errorsCounter.set(0)

        room.roomTask = this
        room.initialize()
    }

    fun removeRoom(room: Room) {
        log.info("Closing room n° {} - name {}", room.roomData.id, room.roomData.caption)

        room.roomUsers.values.toList().forEach { room.removeUser(it, true, true) }

        rooms -= room
        queuedTasks.remove(room)

        room.roomTask = null

        room.saveQueuedItems()
        RoomDao.updateRoomData(room.roomData)
    }

    fun addTask(room: Room, task: IRoomTask) {
        queuedTasks[room]?.offer(task)
    }

    override fun run() {
        try {
            rooms.forEach { room ->
                HabboServer.serverExecutor.execute {
                    try {
                        val queue = queuedTasks[room] ?: return@execute

                        while (queue.isNotEmpty()) queue.poll().executeTask(room)

                        room.roomUsers.values.let {
                            it.forEach { it.onCycle() }

                            it.filter { it.updateNeeded }.let {
                                if (it.isNotEmpty()) {
                                    room.sendHabboResponse(Outgoing.ROOM_USERS_STATUSES, it)

                                    it.forEach { it.updateNeeded = false }
                                }
                            }
                        }

                        room.roomItems.values.forEach { it.onCycle() }

                        if (room.roomUsers.isEmpty() &&
                                TimeUnit.MILLISECONDS.toSeconds(
                                        (room.emptyCounter.incrementAndGet() * HabboServer.habboConfig.roomTaskConfig.delayMilliseconds).toLong())
                                        >= HabboServer.habboConfig.roomTaskConfig.emptyRoomSeconds) {
                            HabboServer.habboGame.roomManager.roomTaskManager.removeRoomFromTask(room)

                            return@execute
                        }

                        if (room.errorsCounter.get() > 0) room.errorsCounter.set(0)
                    } catch (e: Exception) {
                        log.error("An exception happened on room task, room n° ${room.roomData.id}. Cause: {}", e)

                        if (room.errorsCounter.incrementAndGet() > HabboServer.habboConfig.roomTaskConfig.errorThreshold) {
                            log.error("Forcing close of room n° {} since it crashed over {} times!", room.roomData.id, HabboServer.habboConfig.roomTaskConfig.errorThreshold)

                            HabboServer.habboGame.roomManager.roomTaskManager.removeRoomFromTask(room)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Error in room task!", e)
        }
    }
}