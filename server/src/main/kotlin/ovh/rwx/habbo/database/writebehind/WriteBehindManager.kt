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

package ovh.rwx.habbo.database.writebehind

import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.database.user.UserPreferencesDao
import ovh.rwx.habbo.database.user.UserStatsDao
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-concurrency Write-Behind (Deferred Persistence) Manager.
 * Collects dirty entities and asynchronous write operations in memory and
 * periodically persists them using Kotlin Coroutines on Dispatchers.IO.
 *
 * Guaranteed immediate flushes occur on:
 * - User logout / session disconnect
 * - Room unload / save
 * - Server graceful shutdown
 */
object WriteBehindManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    // Unified dirty set — entities self-persist via AbstractDirtyEntity.flush()
    private val dirtyEntities = ConcurrentHashMap.newKeySet<AbstractDirtyEntity>()

    // Unified queue for asynchronous deferred writes
    private val pendingWrites = ConcurrentLinkedQueue<() -> Unit>()

    // Coroutine worker state
    private var workerJob: Job? = null
    private val running = AtomicBoolean(false)

    fun start(scope: CoroutineScope, flushIntervalMs: Long = 2000L) {
        if (running.compareAndSet(false, true)) {
            log.info("Starting Write-Behind persistence worker (interval: ${flushIntervalMs}ms)...")
            workerJob = scope.launch(Dispatchers.IO + CoroutineName("write-behind-worker")) {
                while (isActive && running.get()) {
                    try {
                        delay(flushIntervalMs)
                        flushDirtyEntities()
                    } catch (e: CancellationException) {
                        break
                    } catch (e: Exception) {
                        log.error("Unexpected error in Write-Behind persistence loop", e)
                    }
                }
            }
        }
    }

    fun stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Stopping Write-Behind persistence worker...")
            workerJob?.cancel()
            flushAllSync()
        }
    }

    // --- Entity & Write Action Registration ---

    fun markDirty(entity: AbstractDirtyEntity) {
        dirtyEntities.add(entity)
    }

    fun queue(action: () -> Unit) {
        pendingWrites.add(action)
    }

    // --- Targeted Flush Methods ---

    fun flushSession(session: HabboSession, online: Boolean = true, ip: String = "") {
        if (!session.authenticated) return
        try {
            val user = session.userInformation
            if (dirtyEntities.remove(user) || !online) {
                UserInformationDao.saveInformation(user, online, ip.ifBlank { session.uniqueID })
                user.markClean()
            }
            val stats = session.userStats
            if (dirtyEntities.remove(stats) || !online) {
                UserStatsDao.saveStats(stats)
                stats.markClean()
            }
            val pref = session.userPreferences
            if (dirtyEntities.remove(pref)) {
                UserPreferencesDao.savePreferences(pref)
                pref.markClean()
            }
        } catch (e: Exception) {
            log.error("Failed to flush session data for user ${session.userInformation.id}", e)
        }
    }

    fun flushRoom(room: Room) {
        val roomId = room.roomData.id
        try {
            if (dirtyEntities.remove(room.roomData)) {
                RoomDao.updateRoomData(room.roomData)
                room.roomData.markClean()
            }

            val roomItemsToSave = mutableListOf<RoomItem>()
            val iter = dirtyEntities.iterator()
            while (iter.hasNext()) {
                val entity = iter.next()
                if (entity is RoomItem && entity.roomId == roomId) {
                    iter.remove()
                    roomItemsToSave.add(entity)
                }
            }
            if (roomItemsToSave.isNotEmpty()) {
                RoomDao.saveItems(roomId, roomItemsToSave)
                val wireds = roomItemsToSave.filter {
                    it.furnishing.interactionType.name.startsWith("WIRED_") && it.wiredData != null
                }
                if (wireds.isNotEmpty()) ItemDao.saveWireds(wireds)
                roomItemsToSave.forEach { it.markClean() }
            }
        } catch (e: Exception) {
            log.error("Failed to flush room data for room $roomId", e)
        }
    }

    // --- Periodic & Global Flush ---

    private fun flushDirtyEntities() {
        if (log.isDebugEnabled) {
            val d = dirtyEntities.size
            val p = pendingWrites.size
            if (d > 0 || p > 0) log.debug("Write-Behind flush — dirty: {}, pending writes: {}", d, p)
        }
        flushPendingWrites()
        flushDirtySet()
    }

    private fun flushPendingWrites() {
        if (pendingWrites.isEmpty()) return
        while (true) {
            val action = pendingWrites.poll() ?: break
            try {
                action()
            } catch (e: Exception) {
                log.error("Write-Behind error executing deferred write action", e)
            }
        }
    }

    private fun flushDirtySet() {
        if (dirtyEntities.isEmpty()) return
        val snapshot = ArrayList<AbstractDirtyEntity>()
        val iter = dirtyEntities.iterator()
        while (iter.hasNext()) {
            snapshot.add(iter.next())
            iter.remove()
        }

        // RoomItems in the same room are batched for efficiency; all other entities self-flush polymorphically
        val (items, others) = snapshot.partition { it is RoomItem }
        others.forEach { entity ->
            try {
                entity.flush()
            } catch (e: Exception) {
                log.error("Write-Behind error flushing dirty entity ${entity.javaClass.simpleName}", e)
            }
        }

        if (items.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            (items as List<RoomItem>).groupBy { it.roomId }.forEach { (roomId, roomItems) ->
                try {
                    if (roomId > 0) RoomDao.saveItems(roomId, roomItems)
                    val wireds = roomItems.filter {
                        it.furnishing.interactionType.name.startsWith("WIRED_") && it.wiredData != null
                    }
                    if (wireds.isNotEmpty()) ItemDao.saveWireds(wireds)
                    roomItems.forEach { it.markClean() }
                } catch (e: Exception) {
                    log.error("Write-Behind error saving RoomItems roomId=$roomId", e)
                }
            }
        }
    }

    fun flushAllSync() {
        log.info("Flushing all pending Write-Behind entities synchronously...")
        try {
            while (pendingWrites.isNotEmpty() || dirtyEntities.isNotEmpty()) {
                flushDirtyEntities()
            }
            log.info("Write-Behind synchronous flush complete.")
        } catch (e: Exception) {
            log.error("Error during Write-Behind synchronous flush", e)
        }
    }
}
