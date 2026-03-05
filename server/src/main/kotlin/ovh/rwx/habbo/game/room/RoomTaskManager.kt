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

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ovh.rwx.habbo.HabboServer
import java.util.concurrent.ConcurrentHashMap

class RoomTaskManager {
    private val taskJobs: MutableMap<RoomTask, Job> = ConcurrentHashMap()
    val rooms: MutableSet<Room> = HashSet()

    // O novo "pulso" global do Habbo (50ms = 20 TPS)
    private val baseTickRateMs = 50L

    fun addRoomToTask(room: Room) {
        if (!rooms.add(room)) return

        val tmpTasks = taskJobs.keys.filter { it.rooms.size < HabboServer.habboConfig.roomTaskConfig.maxRoomPerThread }
        val roomTask = if (tmpTasks.isNotEmpty()) tmpTasks.random() else RoomTask()

        // Se a RoomTask é nova e não tem um loop rodando, nós o iniciamos
        if (!taskJobs.containsKey(roomTask)) {
            taskJobs[roomTask] = startTaskLoop(roomTask)
        }

        roomTask.addRoom(room)
    }

    fun removeRoomFromTask(room: Room) {
        if (!rooms.remove(room)) return

        taskJobs.keys.filter { it.rooms.contains(room) }.forEach { roomTask ->
            roomTask.removeRoom(room)

            // Otimização: Se a task ficar vazia, matamos o loop (Coroutine) para poupar CPU
            if (roomTask.rooms.isEmpty()) {
                taskJobs.remove(roomTask)?.cancel()
            }
        }
    }

    private fun startTaskLoop(roomTask: RoomTask): Job {
        return HabboServer.applicationScope.launch {
            while (isActive) {
                val startTime = System.currentTimeMillis()

                // Executa a lógica da sala
                roomTask.run()

                // Game Loop com Delta Time Constante:
                // Calcula quanto tempo a execução demorou e subtrai dos 50ms.
                // Isso evita que o tempo de execução se acumule e atrase o servidor.
                val executionTime = System.currentTimeMillis() - startTime
                val sleepTime = (baseTickRateMs - executionTime).coerceAtLeast(1L)

                delay(sleepTime)
            }
        }
    }
}