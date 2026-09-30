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
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.database.group.GroupDao
import ovh.rwx.habbo.database.pet.PetDao
import ovh.rwx.habbo.database.room.RoomBanDao
import ovh.rwx.habbo.database.room.RoomBanEntry
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.game.group.Group
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.wired.trigger.EmptyTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.PeriodicTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.*
import ovh.rwx.habbo.game.room.gamemap.RoomGamemap
import ovh.rwx.habbo.game.room.games.RoomGameManager
import ovh.rwx.habbo.game.room.games.RoomGameType
import ovh.rwx.habbo.game.room.managers.RoomItemManager
import ovh.rwx.habbo.game.room.managers.RoomNetworkDispatcher
import ovh.rwx.habbo.game.room.managers.RoomUserManager
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.game.room.trading.TradeManager
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomPet
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.room.wired.WiredErrorLogger
import ovh.rwx.habbo.game.room.wired.WiredPerformanceMonitor
import ovh.rwx.habbo.game.room.wired.WiredRoomSettings
import ovh.rwx.habbo.game.room.wired.WiredVariableManager
import ovh.rwx.habbo.pathfinding.IFinder
import ovh.rwx.habbo.pathfinding.core.DiagonalMovement
import ovh.rwx.habbo.pathfinding.core.finders.AStarFinder
import ovh.rwx.habbo.pathfinding.core.heuristics.EuclideanHeuristic
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class Room(val roomData: RoomData, var roomModel: RoomModel) : IHabboResponseSerialize {
    private val log = LoggerFactory.getLogger(javaClass)
    private val stressLog = LoggerFactory.getLogger("ovh.rwx.habbo.stress")

    // region Managers
    val networkDispatcher = RoomNetworkDispatcher(this)
    val userManager = RoomUserManager(this)
    val itemManager = RoomItemManager(this)
    val tradeManager = TradeManager(this)
    val wiredVariableManager = WiredVariableManager(this)
    val wiredPerformanceMonitor = WiredPerformanceMonitor(this)
    val wiredErrorLogger = WiredErrorLogger(this)
    var wiredRoomSettings = WiredRoomSettings()
    // endregion

    // region Game Loop
    private var loopJob: Job? = null
    private var tickCounter = 0
    private val highFrequencyQueue = ConcurrentLinkedQueue<IRoomTask>()
    private val majorTickQueue = ConcurrentLinkedQueue<IRoomTask>()

    val running: Boolean get() = loopJob?.isActive == true
    // endregion

    // region Counters & State
    val rollerCounter = AtomicInteger()
    val emptyCounter = AtomicInteger()
    val errorsCounter = AtomicInteger()
    val roomTimer = AtomicInteger()
    val hostingCounter = AtomicInteger()
    var initialized: Boolean = false
        private set

    val rolledItemsThisTick = CopyOnWriteArraySet<Int>()
    val rolledUsersThisTick = CopyOnWriteArraySet<Int>()
    // endregion

    // region Sub-Managers & Helpers
    lateinit var roomGamemap: RoomGamemap
    val pathfinder: IFinder by lazy { AStarFinder(DiagonalMovement.ALWAYS, EuclideanHeuristic()) }
    val gameManager: RoomGameManager by lazy { RoomGameManager(this) }
    // endregion

    val wordFilter: MutableSet<String> by lazy { HashSet(RoomDao.getWordFilter(roomData.id)) }
    val bannedUsers: MutableMap<Int, RoomBanEntry> by lazy {
        ConcurrentHashMap(RoomBanDao.getBansByRoomId(roomData.id).associateBy { it.userId })
    }

    fun isBanned(userId: Int): Boolean {
        val ban = bannedUsers[userId] ?: return false
        val now = System.currentTimeMillis() / 1000
        if (ban.expireTimestamp <= now) {
            bannedUsers.remove(userId)
            RoomBanDao.removeBan(roomData.id, userId)
            return false
        }
        return true
    }

    fun banUser(userId: Int, username: String, durationSeconds: Long) {
        val now = System.currentTimeMillis() / 1000
        val expireTimestamp = if (durationSeconds == Long.MAX_VALUE) Long.MAX_VALUE else now + durationSeconds
        val entry = RoomBanEntry(userId, username, expireTimestamp)
        bannedUsers[userId] = entry
        RoomBanDao.addBan(roomData.id, userId, expireTimestamp)
    }

    fun unbanUser(userId: Int) {
        bannedUsers.remove(userId)
        RoomBanDao.removeBan(roomData.id, userId)
    }

    val group: Group? get() = if (roomData.groupId == 0) null else HabboServer.habboGame.groupManager.groups[roomData.groupId]
    val loadedGroups: MutableSet<Group> by lazy { CopyOnWriteArraySet() }

    // region Initialization
    fun initialize() {
        if (initialized) return

        itemManager.loadItems()
        roomGamemap = RoomGamemap(this)
        itemManager.triggerItems()
        wiredVariableManager.loadPersistentVariables()

        if (itemManager.items.values.any { it.furnishing.interactionType.name.startsWith("BATTLE_BANZAI") }) {
            gameManager.registerGame(RoomGameType.BATTLE_BANZAI)
        }

        group?.let { loadedGroups.add(it) }

        if (roomData.allowPets) {
            userManager.loadPets()
        }

        initialized = true
    }
    // endregion

    // region Task API
    fun addTask(task: IRoomTask) {
        if (task.highFrequency) {
            highFrequencyQueue.offer(task)
        } else {
            majorTickQueue.offer(task)
        }
    }
    // endregion

    // region Game Loop
    fun startLoop() {
        if (running) return

        log.info("Loading room n° {} - name {}", roomData.id, roomData.name)

        resetCounters()
        initialize()
        roomGamemap.clearEntities()

        val stressTest = HabboServer.habboConfig.stressTest
        // Stress-test ring buffers (only allocated when stressTest == true)
        val tickProcessingBuffer = if (stressTest) LongArray(200) else LongArray(0)
        val majorTickIntervalBuffer = if (stressTest) LongArray(20) else LongArray(0)
        var stressTickIdx = 0
        var stressMajorIdx = 0
        var stressTickCount = 0
        var stressMajorCount = 0
        var stressLogCounter = 0
        var lastMajorTickNano = 0L

        loopJob = HabboServer.applicationScope.launch {
            while (isActive) {
                val startNano = System.nanoTime()
                var isMajorTick = false

                try {
                    tickCounter = (tickCounter + 1) % 10
                    isMajorTick = tickCounter == 0

                    processRoomTick(isMajorTick)

                    if (errorsCounter.get() > 0) errorsCounter.set(0)
                } catch (e: Exception) {
                    handleException(e)
                }

                val elapsedNanos = System.nanoTime() - startNano
                val elapsedMs = elapsedNanos / 1_000_000

                if (stressTest) {
                    tickProcessingBuffer[stressTickIdx] = elapsedMs
                    stressTickIdx = (stressTickIdx + 1) % tickProcessingBuffer.size
                    stressTickCount++
                    stressLogCounter++

                    if (isMajorTick) {
                        val nowNano = System.nanoTime()
                        if (lastMajorTickNano > 0) {
                            val interval = (nowNano - lastMajorTickNano) / 1_000_000
                            majorTickIntervalBuffer[stressMajorIdx] = interval
                            stressMajorIdx = (stressMajorIdx + 1) % majorTickIntervalBuffer.size
                            stressMajorCount++
                        }
                        lastMajorTickNano = nowNano
                    }

                    // Log rolling summary every ~10 seconds (200 ticks * 50ms)
                    if (stressLogCounter >= 200) {
                        stressLogCounter = 0
                        val procCount = minOf(stressTickCount, tickProcessingBuffer.size)
                        val (procAvg, procP95, procMax) = computeStats(tickProcessingBuffer, procCount)

                        val majCount = minOf(stressMajorCount, majorTickIntervalBuffer.size)
                        var majInfo = "n/a"
                        if (majCount >= 2) {
                            val (majAvg, majP95, majMax) = computeStats(majorTickIntervalBuffer, majCount)
                            val drift = majAvg - 500.0
                            majInfo = "avg=%.1fms p95=%dms max=%dms drift=%.1fms".format(majAvg, majP95, majMax, drift)
                        }

                        val procInfo = "avg=%.1fms p95=%dms max=%dms".format(procAvg, procP95, procMax)

                        stressLog.info(
                            "[stress-test] room={} ticks={} proc: {} | major-interval: {}",
                            roomData.id, procCount, procInfo, majInfo
                        )
                    }
                }

                delay((50L - elapsedMs).coerceAtLeast(1L).milliseconds)
            }
        }
    }

    fun stopLoop() {
        if (!running) return

        log.info("Closing room n° {} - name {}", roomData.id, roomData.name)

        tradeManager.clearAllTrades()

        userManager.entities.values.toList().forEach {
            userManager.removeEntity(it, notifyClient = true, kickNotification = true)
        }

        // Baús: encerra trades de depósito pendentes (estado aberto é efêmero, banco sempre fechado)
        HabboServer.habboGame.chestManager.onRoomUnload(this)

        loopJob?.cancel()
        loopJob = null

        highFrequencyQueue.clear()
        majorTickQueue.clear()
        resetCounters()
        roomGamemap.clearEntities()
        saveRoom()
        wiredErrorLogger.dispose()
    }

    private fun resetCounters() {
        emptyCounter.set(0)
        errorsCounter.set(0)
        rollerCounter.set(0)
        roomTimer.set(0)
    }

    private fun computeStats(buffer: LongArray, count: Int): Triple<Double, Long, Long> {
        val times = LongArray(count)
        for (i in 0 until count) times[i] = buffer[i]
        val sorted = times.sorted()
        return Triple(
            sorted.average(),
            sorted[(count * 0.95).toInt().coerceIn(0, count - 1)],
            sorted.last(),
        )
    }
    // endregion

    // region Tick Processing
    private fun processRoomTick(isMajorTick: Boolean) {
        // High-frequency tasks run every tick (50ms)
        drainQueue(highFrequencyQueue)

        // Major tick tasks run every 10th tick (500ms)
        if (isMajorTick) drainQueue(majorTickQueue)

        processWiredsAndGames(isMajorTick)
        processItems(isMajorTick)

        if (isMajorTick) {
            roomTimer.incrementAndGet()
            processHostingAchievement()
            processEntities()
            checkEmptyRoomUnload()
        }
    }

    private fun drainQueue(queue: ConcurrentLinkedQueue<IRoomTask>) {
        val count = queue.size
        repeat(count) {
            val task = queue.poll() ?: return
            task.executeTask(this)
        }
    }

    private fun processWiredsAndGames(isMajorTick: Boolean) {
        itemManager.wiredHandler.triggerWired(WiredTriggerPeriodically::class, null, PeriodicTriggerData)
        itemManager.wiredHandler.triggerWired(WiredTriggerPeriodicallyShort::class, null, PeriodicTriggerData)
        itemManager.wiredHandler.triggerWired(WiredTriggerPeriodicallyLong::class, null, PeriodicTriggerData)
        itemManager.wiredHandler.triggerWired(WiredTriggerAtGivenTime::class, null, EmptyTriggerData)
        itemManager.wiredHandler.triggerWired(WiredTriggerAtTimeLong::class, null, EmptyTriggerData)

        if (isMajorTick) gameManager.tick()
    }

    private fun processHostingAchievement() {
        if (hostingCounter.incrementAndGet() < 120) return

        hostingCounter.set(0)

        val guestCount = userManager.entities.values.filterIsInstance<RoomUser>().count {
            it.habboSession.userInformation.id != roomData.ownerId
        }

        if (guestCount > 0) {
            val ownerSession = HabboServer.habboSessionManager.getHabboSessionById(roomData.ownerId)
            HabboServer.habboGame.achievementManager.progress(
                ownerSession, roomData.ownerId, "ACH_RoomDecoHosting", 1, accumulate = true
            )
        }
    }

    private fun processItems(isMajorTick: Boolean) {
        val items = itemManager.items.values

        val isRollerTick = isMajorTick &&
                rollerCounter.incrementAndGet() >= HabboServer.habboConfig.timerConfig.roller
        if (isRollerTick) {
            rollerCounter.set(0)
            rolledItemsThisTick.clear()
            rolledUsersThisTick.clear()
        }

        for (item in items) {
            if (item.furnishing.interactionType == InteractionType.ROLLER) {
                if (isRollerTick) item.furnishing.interactor?.processTick(this, item)
            } else {
                item.processTick()
            }
        }
    }

    private fun processEntities() {
        val entities = userManager.entities.values
        val needingUpdate = ArrayList<RoomEntity>(entities.size)

        for (entity in entities) {
            // Limpeza de qualquer entidade com sessão fechada (fantasma)
            if (entity is RoomUser && (!entity.habboSession.channel.isActive || !entity.habboSession.channel.isOpen)) {
                log.warn(
                    "Limpando usuário fantasma {} (id={}) da sala {}",
                    entity.habboSession.userInformation.username, entity.virtualID, roomData.id
                )
                userManager.removeEntity(entity, notifyClient = false, kickNotification = false)
                continue
            }

            entity.processTick()

            if (entity.updateNeeded) {
                needingUpdate.add(entity)
                entity.updateNeeded = false
            }
        }

        if (needingUpdate.isNotEmpty()) {
            sendResponse(Outgoing.ROOM_USERS_STATUSES, OutgoingR63A.ROOM_USERS_STATUSES, needingUpdate)
            needingUpdate.forEach { it.updateNeeded = false }
        }
    }

    private fun checkEmptyRoomUnload() {
        if (userManager.entities.values.filterIsInstance<RoomUser>().isNotEmpty()) {
            emptyCounter.set(0)
            return
        }

        val emptySeconds = TimeUnit.MILLISECONDS.toSeconds(emptyCounter.incrementAndGet() * 500L)

        if (emptySeconds >= HabboServer.habboConfig.roomTaskConfig.emptyRoomSeconds) {
            HabboServer.habboGame.roomManager.roomTaskManager.removeRoom(this)
        }
    }

    private fun handleException(e: Exception) {
        log.error("An exception happened on room n° ${roomData.id}. Cause: {}", e.message, e)

        if (errorsCounter.incrementAndGet() > HabboServer.habboConfig.roomTaskConfig.errorThreshold) {
            log.error(
                "Forcing close of room n° {} since it crashed over {} times!",
                roomData.id,
                HabboServer.habboConfig.roomTaskConfig.errorThreshold
            )
            HabboServer.habboGame.roomManager.roomTaskManager.removeRoom(this)
        }
    }
    // endregion

    // region Network / Broadcast
    fun sendHabboResponse(habboResponse: HabboResponse) {
        networkDispatcher.sendResponse(habboResponse)
    }

    fun sendHabboResponse(outgoing: Outgoing, vararg args: Any?) {
        networkDispatcher.sendResponseModern(outgoing, *args)
    }

    fun sendHabboResponse(outgoing: OutgoingR63A, vararg args: Any?) {
        networkDispatcher.sendResponseR63A(outgoing, *args)
    }

    fun sendResponse(outgoing: Outgoing?, outgoingR63A: OutgoingR63A?, vararg args: Any?) {
        networkDispatcher.sendResponse(outgoing, outgoingR63A, *args)
    }
    // endregion

    fun updateGroupInfo() {
        userManager.updateGroupInfo()
    }

    fun updateGroupRights() {
        userManager.updateGroupRights()
    }

    fun saveRoom() {
        RoomDao.updateRoomData(roomData)
        group?.let { GroupDao.updateGroupData(it.groupData) }
        itemManager.savePendingItems()
        wiredVariableManager.savePersistentVariables()

        // Save pet positions
        userManager.entities.values.filterIsInstance<RoomPet>().forEach { roomPet ->
            roomPet.syncPosition()
            PetDao.savePet(roomPet.petData)
        }
    }

    // region Serialization
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            val enterRoom = params.getOrNull(1) as? Boolean ?: false

            writeInt(roomData.id)
            writeUTF(roomData.name)
            writeInt(roomData.ownerId)
            writeUTF(roomData.ownerName)
            writeInt(roomData.state.state)
            writeInt(userManager.entities.values.filterIsInstance<RoomUser>().size)
            writeInt(roomData.usersMax)
            writeUTF(roomData.description)
            writeInt(roomData.tradeState)
            writeInt(roomData.score)
            writeInt(0) // ranking
            writeInt(roomData.category)

            writeInt(roomData.tags.size)
            roomData.tags.forEach { writeUTF(it) }

            var bitMaskValue = if (enterRoom) 32 else 0
            if (group != null) bitMaskValue += 2
            if (roomData.roomType == RoomType.PRIVATE) bitMaskValue += 8
            if (roomData.allowPets) bitMaskValue += 16

            writeInt(bitMaskValue)

            group?.let {
                writeInt(it.groupData.id)
                writeUTF(it.groupData.name)
                writeUTF(it.groupData.badge)
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(roomData.id)
            writeBoolean(false) // is event
            writeUTF(roomData.name)
            writeUTF(roomData.ownerName)
            writeInt(roomData.state.state)
            writeInt(userManager.entities.values.filterIsInstance<RoomUser>().size)
            writeInt(roomData.usersMax)
            writeUTF(roomData.description)
            writeInt(roomData.tradeState) // srchSpecPrm
            writeBoolean(roomData.tradeState == 1) // allowTrading
            writeInt(roomData.score)

            // Revelação R37: Categoria e Tempo de Evento entraram em Agosto de 2009!
            if (isVersionAtLeast(2009, 8, 21)) {
                writeInt(roomData.category)
                writeUTF("") // eventCreationTime
            }

            writeInt(roomData.tags.size)
            roomData.tags.forEach { writeUTF(it) }

            // A Classe de Miniaturas (Ícones de Quarto antigos do Shockwave)
            // Isso existe em todas as versões (O LOOP de Int/Int dentro do trace)
            // Mandar '0' no bg_count aborta a classe lindamente.
            writeInt(1) // bg
            writeInt(0) // fg
            writeInt(0) // count

            // Revelação R40: Animais foram liberados nos quartos em Nov/2009
            if (isVersionAtLeast(2009, 11, 13)) {
                writeBoolean(roomData.allowPets)
            }

            if (isVersionAtLeast(2011, 4, 18)) {
                writeBoolean(roomData.allowPetsEat)
            }
        }
    }
    // endregion
}
