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

import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.wired.trigger.EmptyTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.PeriodicTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerAtGivenTime
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerPeriodically
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerPeriodicallyLong
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerPeriodicallyShort
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RoomTask : Runnable {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val rooms: MutableSet<Room> = CopyOnWriteArraySet()
    private val queuedTasks: MutableMap<Room, Queue<IRoomTask>> = ConcurrentHashMap()

    // Novo contador para gerenciar os sub-ticks (0 a 9)
    private val tickCounter = AtomicInteger(0)

    // --- GERENCIAMENTO DE QUARTOS ---
    fun addRoom(room: Room) {
        if (!rooms.add(room)) return

        log.info("Loading room n° {} - name {}", room.roomData.id, room.roomData.name)

        queuedTasks[room] = ConcurrentLinkedQueue()

        resetRoomCounters(room)
        room.roomTask = this

        room.initialize()
        room.roomGamemap.clearUsers()
    }

    fun removeRoom(room: Room) {
        if (!rooms.remove(room)) return

        log.info("Closing room n° {} - name {}", room.roomData.id, room.roomData.name)

        room.roomTask = null

        // Cancela todas as trocas ativas antes de remover os usuários
        room.tradeManager.clearAllTrades()
        
        room.userManager.users.values.toList().forEach {
            room.userManager.removeUser(it, notifyClient = true, kickNotification = true)
        }

        queuedTasks.remove(room)
        resetRoomCounters(room)
        room.roomGamemap.clearUsers()
        room.saveRoom()
    }

    private fun resetRoomCounters(room: Room) {
        room.emptyCounter.set(0)
        room.errorsCounter.set(0)
        room.rollerCounter.set(0)
        room.roomTimer.set(0)
    }

    fun addTask(room: Room, task: IRoomTask) {
        queuedTasks[room]?.offer(task)
    }

    // --- CICLO PRINCIPAL ---
    override fun run() {
        try {
            // Avança o tick global e reseta a cada 10 execuções (0-9)
            val currentTick = tickCounter.getAndUpdate { if (it >= 9) 0 else it + 1 }
            val isMajorTick = currentTick == 0 // Roda a cada 500ms

            rooms.forEach { room ->
                HabboServer.applicationScope.launch {
                    processRoomSafe(room, isMajorTick)
                }
            }
        } catch (e: Exception) {
            log.error("Error dispatching room tasks!", e)
        }
    }

    private fun processRoomSafe(room: Room, isMajorTick: Boolean) {
        try {
            processRoomTick(room, isMajorTick)

            if (room.errorsCounter.get() > 0) {
                room.errorsCounter.set(0)
            }
        } catch (e: Exception) {
            handleRoomException(room, e)
        }
    }

    // --- ETAPAS DO CICLO ---
    private fun processRoomTick(room: Room, isMajorTick: Boolean) {
        // Tarefas de fila e Wireds rodam RÁPIDO (50ms)
        processQueuedTasks(room, isMajorTick)
        processWiredsAndGames(room, isMajorTick)

        // Passamos a responsabilidade de dividir o tempo para o próprio método
        processItems(room, isMajorTick)

        // As tarefas pesadas e legadas rodam DEVAGAR (500ms)
        if (isMajorTick) {
            room.roomTimer.incrementAndGet()
            processHostingAchievement(room)
            processUsers(room)
            checkEmptyRoomUnload(room)
        }
    }

    private fun processQueuedTasks(room: Room, isMajorTick: Boolean) {
        val queue = queuedTasks[room] ?: return
        val size = queue.size

        // Processamos a fila atual. Se a tarefa não for para agora, ela volta para o fim da fila.
        for (i in 0 until size) {
            val task = queue.poll() ?: break

            if (task.highFrequency || isMajorTick) {
                task.executeTask(room)
            } else {
                // Se não é alta frequência e não é o Major Tick (500ms), devolvemos para a fila
                queue.offer(task)
            }
        }
    }

    private fun processWiredsAndGames(room: Room, isMajorTick: Boolean) {
        // Estas chamadas agora são processadas a cada 50ms.
        // É essencial que a lógica interna dessas classes do Wired
        // controle seus próprios delays (se precisarem) baseados neste novo ritmo.
        room.itemManager.wiredHandler.triggerWired(WiredTriggerPeriodically::class, null, PeriodicTriggerData)
        room.itemManager.wiredHandler.triggerWired(WiredTriggerPeriodicallyShort::class, null, PeriodicTriggerData)
        room.itemManager.wiredHandler.triggerWired(WiredTriggerPeriodicallyLong::class, null, PeriodicTriggerData)
        room.itemManager.wiredHandler.triggerWired(WiredTriggerAtGivenTime::class, null, EmptyTriggerData)

        if (isMajorTick) {
            room.gameManager.tick()
        }
    }

    private fun processHostingAchievement(room: Room) {
        // Como isso roda a cada Major Tick (500ms), 120 ciclos continua sendo 1 minuto.
        if (room.hostingCounter.incrementAndGet() < 120) return

        room.hostingCounter.set(0)

        val guestCount = room.userManager.users.values.count {
            it.habboSession != null && it.habboSession.userInformation.id != room.roomData.ownerId
        }

        if (guestCount > 0) {
            val ownerSession = HabboServer.habboSessionManager.getHabboSessionById(room.roomData.ownerId)
            HabboServer.habboGame.achievementManager.progress(
                ownerSession,
                room.roomData.ownerId,
                "ACH_RoomDecoHosting",
                1,
                accumulate = true
            )
        }
    }

    private fun processItems(room: Room, isMajorTick: Boolean) {
        val items = room.itemManager.items.values

        if (isMajorTick) {
            if (room.rollerCounter.incrementAndGet() >= HabboServer.habboConfig.timerConfig.roller) {
                room.rollerCounter.set(0)

                room.rolledItemsThisTick.clear()
                room.rolledUsersThisTick.clear()

                items.filter { it.furnishing.interactionType == InteractionType.ROLLER }
                    .forEach {
                        it.furnishing.interactor?.processTick(room, it)
                    }
            }
        }

        items.filter { it.furnishing.interactionType != InteractionType.ROLLER }
            .forEach { it.processTick() }
    }

    private fun processUsers(room: Room) {
        val users = room.userManager.users.values

        // Limpa usuários pendentes que ficaram presos (disconnect durante o join)
        // Um usuário em pendingJoin por mais de 5 segundos (10 major ticks) é considerado inválido
        val pendingUsersToRemove = users.filter { it.pendingJoin && it.habboSession?.channel?.isOpen == false }
        pendingUsersToRemove.forEach { pendingUser ->
            log.warn(
                "Removing stuck pending user {} from room {} (session closed)",
                pendingUser.habboSession?.userInformation?.username, room.roomData.id
            )
            room.userManager.removeUser(pendingUser, notifyClient = false, kickNotification = false)
        }

        users.forEach { it.processTick() }

        val usersNeedingUpdate = users.filter { it.updateNeeded }
        if (usersNeedingUpdate.isNotEmpty()) {
            room.sendHabboResponse(Outgoing.ROOM_USERS_STATUSES, usersNeedingUpdate)
            room.sendHabboResponse(OutgoingR63A.ROOM_USERS_STATUSES, usersNeedingUpdate)

            usersNeedingUpdate.forEach { it.updateNeeded = false }
        }
    }

    private fun checkEmptyRoomUnload(room: Room) {
        if (room.userManager.users.isNotEmpty()) {
            room.emptyCounter.set(0)
            return
        }

        // Delay configurado no HabboConfig agora deve ser avaliado considerando que
        // essa verificação só ocorre no Major Tick (500ms).
        val emptySeconds = TimeUnit.MILLISECONDS.toSeconds(
            room.emptyCounter.incrementAndGet() * 500L // Forçamos 500L para manter a consistência com o Major Tick
        )

        if (emptySeconds >= HabboServer.habboConfig.roomTaskConfig.emptyRoomSeconds) {
            HabboServer.habboGame.roomManager.roomTaskManager.removeRoomFromTask(room)
        }
    }

    private fun handleRoomException(room: Room, e: Exception) {
        log.error("An exception happened on room task, room n° ${room.roomData.id}. Cause: {}", e.message, e)

        if (room.errorsCounter.incrementAndGet() > HabboServer.habboConfig.roomTaskConfig.errorThreshold) {
            log.error(
                "Forcing close of room n° {} since it crashed over {} times!",
                room.roomData.id,
                HabboServer.habboConfig.roomTaskConfig.errorThreshold
            )
            HabboServer.habboGame.roomManager.roomTaskManager.removeRoomFromTask(room)
        }
    }
}