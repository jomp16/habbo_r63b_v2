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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.database.group.GroupDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.pet.PetDao
import ovh.rwx.habbo.database.room.RoomBanDao
import ovh.rwx.habbo.database.room.RoomBanEntry
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.database.writebehind.WriteKey
import ovh.rwx.habbo.game.HabboGame
import ovh.rwx.habbo.game.group.Group
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.RoomGameKind
import ovh.rwx.habbo.game.item.wired.variable.WiredVariable
import ovh.rwx.habbo.game.item.wired.variable.WiredVariableValue
import ovh.rwx.habbo.game.pet.PetData
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
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.OutboxBatch
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

/** Leituras de banco da carga inicial da sala; [EMPTY] para quarto virtual (nada a ler). */
private class RoomInitialData(
    val roomVariables: List<Pair<WiredVariable, WiredVariableValue>>,
    val furniVariables: List<Pair<WiredVariable, WiredVariableValue>>,
    val pets: List<PetData>,
) {
    companion object {
        val EMPTY = RoomInitialData(emptyList(), emptyList(), emptyList())
    }
}

class Room(val roomData: RoomData, var roomModel: RoomModel) : IHabboResponseSerialize {
    private val log = LoggerFactory.getLogger(javaClass)
    private val slowTickLog = LoggerFactory.getLogger("ovh.rwx.habbo.room.slowtick")

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
    internal var tickCounter = 0
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

    // Quarto virtual: filtro e bans só em memória (não há linha no banco para referenciar)
    val wordFilter: MutableSet<String> by lazy {
        if (roomData.virtual) HashSet() else HashSet(RoomDao.getWordFilter(roomData.id))
    }
    val bannedUsers: MutableMap<Int, RoomBanEntry> by lazy {
        if (roomData.virtual) ConcurrentHashMap()
        else ConcurrentHashMap(RoomBanDao.getBansByRoomId(roomData.id).associateBy { it.userId })
    }

    fun isBanned(userId: Int): Boolean {
        val ban = bannedUsers[userId] ?: return false
        val now = System.currentTimeMillis() / 1000
        if (ban.expireTimestamp <= now) {
            bannedUsers.remove(userId)
            if (!roomData.virtual) RoomBanDao.removeBan(roomData.id, userId)
            return false
        }
        return true
    }

    fun banUser(userId: Int, username: String, durationSeconds: Long) {
        val now = System.currentTimeMillis() / 1000
        val expireTimestamp = if (durationSeconds == Long.MAX_VALUE) Long.MAX_VALUE else now + durationSeconds
        val entry = RoomBanEntry(userId, username, expireTimestamp)
        bannedUsers[userId] = entry
        if (!roomData.virtual) RoomBanDao.addBan(roomData.id, userId, expireTimestamp)
    }

    fun unbanUser(userId: Int) {
        bannedUsers.remove(userId)
        if (!roomData.virtual) RoomBanDao.removeBan(roomData.id, userId)
    }

    val group: Group? get() = if (roomData.groupId == 0) null else HabboGame.instance?.groupManager?.groups?.get(roomData.groupId)
    val loadedGroups: MutableSet<Group> by lazy { CopyOnWriteArraySet() }

    // region Initialization
    @Synchronized
    fun initialize() {
        if (initialized) return

        // Leituras de banco independentes em paralelo (antes eram 7 idas em série na thread do handler):
        // fase 1 = itens(+wired) ‖ variáveis da sala ‖ bans ‖ pets; fase 2 = variáveis dos mobis (precisam dos ids dos itens)
        // Quarto virtual (stress test): nada a ler do banco — sem itens, variáveis, bans nem pets
        val loadPets = roomData.allowPets && !roomData.virtual
        val loaded = if (roomData.virtual) RoomInitialData.EMPTY else runBlocking(Dispatchers.IO) {
            val itemsJob = async { itemManager.loadItems() }
            val roomVarsJob = async { wiredVariableManager.fetchRoomVariables() }
            val bansJob = async { bannedUsers.size }
            val petsJob = async { if (loadPets) PetDao.getPetsByRoomId(roomData.id) else emptyList() }

            itemsJob.await()
            val furniVarsJob = async { wiredVariableManager.fetchFurniVariables() }
            val wiredJob = async { itemManager.preloadWiredData() }

            bansJob.await()
            wiredJob.await()
            RoomInitialData(roomVarsJob.await(), furniVarsJob.await(), petsJob.await())
        }

        roomGamemap = RoomGamemap(this)
        itemManager.triggerItems()
        wiredVariableManager.loadPersistentVariables(loaded.roomVariables, loaded.furniVariables)

        if (itemManager.items.values.any { it.furnishing.interactionType.roomGameKind == RoomGameKind.BATTLE_BANZAI }) {
            gameManager.registerGame(RoomGameType.BATTLE_BANZAI)
        }

        group?.let { loadedGroups.add(it) }

        if (loadPets) {
            userManager.loadPets(loaded.pets)
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

        val tickMonitor = RoomTickMonitor(roomData.id, HabboServer.habboConfig.stressTest)

        loopJob = HabboServer.applicationScope.launch {
            while (isActive) {
                val startNano = System.nanoTime()

                try {
                    // as mensagens do tick só agendam o drain das sessões ao fim dele (um por sessão, não um por rajada)
                    OutboxBatch.collect { tick() }

                    if (errorsCounter.get() > 0) errorsCounter.set(0)
                } catch (e: Exception) {
                    handleException(e)
                }

                val elapsedNanos = System.nanoTime() - startNano
                val elapsedMs = elapsedNanos / 1_000_000

                tickMonitor.onTick(startNano, elapsedNanos, tickCounter == 0)

                val delayMs = (50L - elapsedMs).coerceAtLeast(1L)
                val delayStartNano = System.nanoTime()
                delay(delayMs.milliseconds)

                // quanto acordou depois do que pediu: pool ocupado, GC ou safepoint (o tick em si não entra nessa conta)
                tickMonitor.onSchedulingLag((System.nanoTime() - delayStartNano - delayMs * 1_000_000L).coerceAtLeast(0L))
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
        HabboGame.instance?.chestManager?.onRoomUnload(this)

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

    // endregion

    // region Tick Processing
    internal fun tick() {
        tickCounter = (tickCounter + 1) % 10
        processRoomTick(tickCounter == 0)
    }

    internal fun processRoomTick(isMajorTick: Boolean) {
        val t0 = System.nanoTime()

        // High-frequency tasks run every tick (50ms)
        drainQueue(highFrequencyQueue)
        val t1 = System.nanoTime()

        // Major tick tasks run every 10th tick (500ms)
        if (isMajorTick) drainQueue(majorTickQueue)
        val t2 = System.nanoTime()

        processWiredsAndGames(isMajorTick)
        val t3 = System.nanoTime()

        var t5 = t3
        var t6 = t3
        var t7 = t3
        if (isMajorTick) {
            roomTimer.incrementAndGet()
            processHostingAchievement()
            t5 = System.nanoTime()

            processEntities()
            t6 = System.nanoTime()

            checkEmptyRoomUnload()
            t7 = System.nanoTime()
        }

        processItems(isMajorTick)
        val t4 = System.nanoTime()

        val totalNanos = maxOf(t7, t4) - t0
        if (totalNanos > 5_000_000L && slowTickLog.isWarnEnabled) {
            val totalMs = totalNanos / 1_000_000.0
            val highMs = (t1 - t0) / 1_000_000.0
            val majorMs = (t2 - t1) / 1_000_000.0
            val wiredMs = (t3 - t2) / 1_000_000.0
            val hostingMs = (t5 - t3) / 1_000_000.0
            val entitiesMs = (t6 - t5) / 1_000_000.0
            val unloadMs = (t7 - t6) / 1_000_000.0
            val itemsMs = (t4 - (if (isMajorTick) t7 else t3)) / 1_000_000.0
            slowTickLog.warn(
                "[slow-tick] room={} tick={} isMajor={} total={}ms (highQueue={}ms, majorQueue={}ms, wireds={}ms, hosting={}ms, entities={}ms, unload={}ms, items={}ms)",
                roomData.id,
                tickCounter,
                isMajorTick,
                "%.2f".format(totalMs),
                "%.2f".format(highMs),
                "%.2f".format(majorMs),
                "%.2f".format(wiredMs),
                "%.2f".format(hostingMs),
                "%.2f".format(entitiesMs),
                "%.2f".format(unloadMs),
                "%.2f".format(itemsMs)
            )
        }
    }

    private fun logSlowSection(label: String, startNano: Long) {
        val elapsed = System.nanoTime() - startNano
        if (elapsed > 5_000_000L && slowTickLog.isWarnEnabled) {
            slowTickLog.warn("[slow-section] room={} {} took {}ms", roomData.id, label, "%.2f".format(elapsed / 1_000_000.0))
        }
    }

    private fun drainQueue(queue: ConcurrentLinkedQueue<IRoomTask>) {
        val count = queue.size
        repeat(count) {
            val task = queue.poll() ?: return
            val start = System.nanoTime()
            task.executeTask(this)
            logSlowSection("task ${task.javaClass.simpleName}", start)
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
        // o "dono" de um quarto virtual não existe: nada a progredir
        if (roomData.virtual) return
        if (hostingCounter.incrementAndGet() < 120) return

        hostingCounter.set(0)

        val guestCount = userManager.entities.values.filterIsInstance<RoomUser>().count {
            it.habboSession.userInformation.id != roomData.ownerId
        }

        if (guestCount > 0) {
            val game = HabboGame.instance ?: return
            val ownerSession = game.sessionManager?.getHabboSessionById(roomData.ownerId)
            game.achievementManager.progress(
                ownerSession, roomData.ownerId, "ACH_RoomDecoHosting", 1, accumulate = true
            )
        }
    }

    private fun processItems(isMajorTick: Boolean) {
        val items = itemManager.items.values

        val rollerInterval = HabboGame.instance?.config?.timerConfig?.roller ?: 4
        val isRollerTick = isMajorTick &&
                rollerCounter.incrementAndGet() >= rollerInterval
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

            val tickStart = System.nanoTime()
            entity.processTick()
            logSlowSection("entity.processTick ${entity.javaClass.simpleName}#${entity.virtualID}", tickStart)

            if (entity.updateNeeded) {
                needingUpdate.add(entity)
                entity.updateNeeded = false
            }
        }

        if (needingUpdate.isNotEmpty()) {
            val sendStart = System.nanoTime()
            sendResponse(Outgoing.ROOM_USERS_STATUSES, OutgoingR63A.ROOM_USERS_STATUSES, needingUpdate)
            logSlowSection("sendResponse ROOM_USERS_STATUSES (${needingUpdate.size} entities)", sendStart)
            needingUpdate.forEach { it.updateNeeded = false }
        }
    }

    private fun checkEmptyRoomUnload() {
        if (userManager.entities.values.filterIsInstance<RoomUser>().isNotEmpty()) {
            emptyCounter.set(0)
            return
        }

        val emptySeconds = TimeUnit.MILLISECONDS.toSeconds(emptyCounter.incrementAndGet() * 500L)
        val emptyThreshold = HabboGame.instance?.config?.roomTaskConfig?.emptyRoomSeconds ?: 60

        if (emptySeconds >= emptyThreshold) {
            HabboGame.instance?.roomManager?.roomTaskManager?.removeRoom(this)
        }
    }

    private fun handleException(e: Exception) {
        log.error("An exception happened on room n° ${roomData.id}. Cause: {}", e.message, e)

        val threshold = HabboGame.instance?.config?.roomTaskConfig?.errorThreshold ?: 10
        if (errorsCounter.incrementAndGet() > threshold) {
            log.error(
                "Forcing close of room n° {} since it crashed over {} times!",
                roomData.id,
                threshold
            )
            HabboGame.instance?.roomManager?.roomTaskManager?.removeRoom(this)
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

    fun sendResponse(outgoing: Outgoing?, outgoingR63A: OutgoingR63A?, vararg args: Any?, excludeSession: HabboSession? = null) {
        networkDispatcher.sendResponse(outgoing, outgoingR63A, *args, excludeSession = excludeSession)
    }
    // endregion

    fun updateGroupInfo() {
        userManager.updateGroupInfo()
    }

    fun updateGroupRights() {
        userManager.updateGroupRights()
    }

    fun saveRoom() {
        // Quarto virtual nunca persiste (nem a linha da sala, nem itens, pets, grupo ou variáveis)
        if (roomData.virtual) return

        val pendingItems = itemManager.drainItemsToSave()
        val dimmer = itemManager.roomDimmer?.takeIf { pendingItems.contains(it.roomItem) }
        dimmer?.let { ItemDao.saveDimmer(it) }

        val petsToSave = userManager.entities.values.filterIsInstance<RoomPet>().map {
            it.syncPosition()
            it.petData
        }

        group?.let { GroupDao.updateGroupData(it.groupData) }
        wiredVariableManager.savePersistentVariables()
        petsToSave.forEach { PetDao.savePet(it) }

        // grava a linha da sala e as linhas dos itens (que apontam para a sala via room_id): declara ambos
        WriteBehindManager.queue(WriteKey.items(pendingItems.map { it.id }) + WriteKey.room(roomData.id)) {
            try {
                RoomDao.updateRoomData(roomData)
                if (pendingItems.isNotEmpty()) {
                    RoomDao.saveItems(roomData.id, pendingItems)
                    val wiredsToSave = pendingItems.filter { it.furnishing.interactionType.isWired && it.wiredData != null }
                    if (wiredsToSave.isNotEmpty()) ItemDao.saveWireds(wiredsToSave)
                }
            } catch (e: Exception) {
                log.error("Failed to save room data asynchronously for room ${roomData.id}", e)
            }
        }
    }

    // region Serialization
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            val enterRoom = params.getOrNull(1) as? Boolean ?: false
            val userCount = userManager.entities.values.filterIsInstance<RoomUser>().size
            val groupData = group?.groupData

            if (isVersionBefore(2012, 10, 15)) {
                // GuestRoomData de 2009-06 a 2012-10, uma só função para o pré-shuffle (R63A) e o pós-shuffle (habbo-timeline
                // ROOM_INFO incoming; RELEASE63-201110051250 _SafeStr_1994 e RELEASE63-201207040943 _SafeStr_2328, verificados no AS3).
                // Transições: 2009-08-21 category + string, 2009-11-13 allowPets, 2010-12-03 (wrapper), 2011-04-18 allowPetsEat,
                // 2011-09-22 groupId, 2011-10-03 groupName + badge, 2011-10-24 ownerId, 2011-11-16 allowTrading Boolean ->
                // tradeState Int, 2012-05-14 showOwnerName, 2012-10-15 o "event" sai e o nome passa a vir antes do Boolean (abaixo).
                writeInt(roomData.id)
                writeBoolean(false) // event
                writeUTF(roomData.name)
                if (isVersionAtLeast(2012, 5, 14)) writeBoolean(true) // showOwnerName
                if (isVersionAtLeast(2011, 10, 24)) writeInt(roomData.ownerId)
                writeUTF(roomData.ownerName)
                writeInt(roomData.state.state) // doorMode: 1=doorbell, 2=password
                writeInt(userCount)
                writeInt(roomData.usersMax)
                writeUTF(roomData.description)
                if (isVersionAtLeast(2011, 11, 16)) {
                    writeInt(0) // srchSpecPrm
                    writeInt(roomData.tradeState)
                } else {
                    writeInt(roomData.tradeState) // srchSpecPrm
                    writeBoolean(roomData.tradeState == 1) // allowTrading
                }
                writeInt(roomData.score)
                if (isVersionAtLeast(2009, 8, 21)) writeInt(roomData.category)
                if (isVersionAtLeast(2011, 9, 22)) writeInt(groupData?.id ?: 0) // >0 faz o cliente pedir os dados do grupo
                if (isVersionAtLeast(2011, 10, 3)) {
                    writeUTF(groupData?.name ?: "")
                    writeUTF(groupData?.badge ?: "")
                }
                if (isVersionAtLeast(2009, 8, 21)) writeUTF("") // eventCreationTime, depois sem uso na UI

                writeInt(roomData.tags.size)
                roomData.tags.forEach { writeUTF(it) }

                // miniatura (ícone de quarto antigo): bg, fg, quantidade
                writeInt(1)
                writeInt(0)
                writeInt(0)
                if (isVersionAtLeast(2009, 11, 13)) writeBoolean(roomData.allowPets)
                if (isVersionAtLeast(2011, 4, 18)) writeBoolean(roomData.allowPetsEat)
                return@apply
            }

            // GuestRoomData antigo (2013-05-22 _SafeStr_1749 == 2014-12-10 _SafeStr_2293, verificado no AS3);
            // o layout morre antes de PRODUCTION-201512012203, que já lê o bitmask condicional moderno
            val oldLayout = isVersionBefore(2015, 12, 1)

            writeInt(roomData.id)
            writeUTF(roomData.name)
            if (oldLayout) {
                // showOwnerName (UI mostra o dono se true && ownerId > 0); no layout moderno migrou para o bit 8 do bitmask
                writeBoolean(true)
            }
            writeInt(roomData.ownerId)
            writeUTF(roomData.ownerName)
            writeInt(roomData.state.state) // doorMode: 1=doorbell, 2=password
            writeInt(userCount)
            writeInt(roomData.usersMax)
            writeUTF(roomData.description)
            if (oldLayout) {
                writeInt(0) // srchSpecPrm, remanescente preshuffle sem uso na UI
            }
            writeInt(roomData.tradeState) // "trading_allowed" na UI
            writeInt(roomData.score)
            writeInt(0) // ranking
            writeInt(roomData.category)
            if (oldLayout) {
                // grupo incondicional de 4 campos antes das tags
                writeInt(groupData?.id ?: 0) // >0 faz o cliente pedir os dados do grupo
                writeUTF(groupData?.name ?: "")
                writeUTF(groupData?.badge ?: "")
                writeUTF("") // string sem uso na UI 2013
            }

            writeInt(roomData.tags.size)
            roomData.tags.forEach { writeUTF(it) }

            if (oldLayout) {
                // rabo extinto no layout moderno: a moderação vive no wrapper externo nas duas eras

                // DTO de moderação embutido: 0/0/0 -> o cliente aplica o default (mute=1, kick=0, ban=(4,1))
                writeInt(0)
                writeInt(0)
                writeInt(0)
                // ponytail: flag de settings do editor de quarto sempre false; mover para roomData quando o fluxo de settings 2013 for endereçado
                writeBoolean(false)
                writeBoolean(false) // roomenterad
                writeUTF("") // eventName
                writeUTF("") // eventDesc
                writeInt(0) // eventMinutes
            } else {
                var bitMaskValue = if (enterRoom) 32 else 0
                if (group != null) bitMaskValue += 2
                if (roomData.roomType == RoomType.PRIVATE) bitMaskValue += 8
                if (roomData.allowPets) bitMaskValue += 16

                writeInt(bitMaskValue)

                groupData?.let {
                    writeInt(it.id)
                    writeUTF(it.name)
                    writeUTF(it.badge)
                }
            }
        }
    }

    // O GuestRoomData tem um só layout versionado (serializeHabboResponse): o pré-shuffle usa o mesmo corpo.
    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) =
        serializeHabboResponse(habboResponse, *params)
    // endregion
}
