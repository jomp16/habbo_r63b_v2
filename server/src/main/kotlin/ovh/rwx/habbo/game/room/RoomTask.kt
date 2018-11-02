/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.room.tasks.WiredDelayTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

class RoomTask : Runnable {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val rooms: MutableSet<Room> = CopyOnWriteArraySet()
    private val queuedTasks: MutableMap<Room, Queue<IRoomTask>> = ConcurrentHashMap()

    fun addRoom(room: Room) {
        if (rooms.contains(room)) return

        log.info("Loading room n° {} - name {}", room.roomData.id, room.roomData.name)

        rooms += room
        queuedTasks[room] = ConcurrentLinkedQueue()

        room.emptyCounter.set(0)
        room.errorsCounter.set(0)
        room.rollerCounter.set(0)

        room.roomTask = this

        room.initialize()
        room.roomGamemap.clearUsers()
    }

    fun removeRoom(room: Room) {
        if (!rooms.contains(room)) return

        log.info("Closing room n° {} - name {}", room.roomData.id, room.roomData.name)

        room.roomTask = null

        room.roomUsers.values.toList().forEach { room.removeUser(it, true, true) }

        rooms -= room
        queuedTasks.remove(room)

        room.emptyCounter.set(0)
        room.errorsCounter.set(0)
        room.rollerCounter.set(0)
        room.roomGamemap.clearUsers()

        // This method already saves room data and group data
        room.saveRoom()
    }

    fun addTask(room: Room, task: IRoomTask) {
        queuedTasks[room]?.offer(task)
    }

    override fun run() {
        try {
            rooms.forEach { room ->
                GlobalScope.launch {
                    try {
                        val queuedTasks = queuedTasks[room] ?: return@launch
                        val wireds = mutableListOf<IRoomTask>()

                        while (queuedTasks.isNotEmpty()) {
                            val task = queuedTasks.poll()

                            if (task is WiredDelayTask) wireds += task
                            else task.executeTask(room)
                        }

                        wireds.forEach { it.executeTask(room) }

                        room.roomItems.values.filter { it.furnishing.interactionType != InteractionType.ROLLER }.forEach { it.onCycle() }

                        if (room.rollerCounter.incrementAndGet() >= HabboServer.habboConfig.timerConfig.roller) {
                            room.rollerCounter.set(0)

                            room.roomItems.values.filter { it.furnishing.interactionType == InteractionType.ROLLER }.forEach { it.onCycle() }
                        }

                        room.roomUsers.values.let {
                            it.forEach { roomUser -> roomUser.onCycle() }

                            it.filter { roomUser -> roomUser.updateNeeded }.let { list ->
                                if (list.isNotEmpty()) {
                                    room.sendHabboResponse(Outgoing.ROOM_USERS_STATUSES, list)

                                    list.forEach { roomUser -> roomUser.updateNeeded = false }
                                }
                            }
                        }

                        if (room.roomUsers.isEmpty() && TimeUnit.MILLISECONDS.toSeconds((room.emptyCounter.incrementAndGet() * HabboServer.habboConfig.roomTaskConfig.delayMilliseconds).toLong()) >= HabboServer.habboConfig.roomTaskConfig.emptyRoomSeconds) {
                            HabboServer.habboGame.roomManager.roomTaskManager.removeRoomFromTask(room)

                            return@launch
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