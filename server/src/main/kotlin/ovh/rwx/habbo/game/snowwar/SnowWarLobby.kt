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

package ovh.rwx.habbo.game.snowwar

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.gamecenter.*
import ovh.rwx.habbo.game.snowwar.enums.SnowWarFieldType
import ovh.rwx.habbo.game.snowwar.enums.SnowWarResultType
import ovh.rwx.habbo.game.snowwar.enums.SnowWarStageState
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTeam
import ovh.rwx.habbo.game.snowwar.objects.*
import ovh.rwx.habbo.game.snowwar.utils.QuickRandom
import ovh.rwx.habbo.game.snowwar.utils.SnowWarMath
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class SnowWarLobby(
    val gameId: Int,
    val levelName: String = "snowwar_stage_1",
    val gameType: Int = 0,
    val fieldType: SnowWarFieldType = SnowWarFieldType.random(),
    val numberOfTeams: Int = 2,
    val maximumPlayers: Int = HabboServer.habboConfig.gameConfig.snowwar.maxPlayers,
    val owningPlayerName: String = ""
) {
    private val log = LoggerFactory.getLogger("SnowWarLobby-$gameId")
    val users = ConcurrentHashMap<Int, SnowWarUser>()
    val machines = CopyOnWriteArrayList<SnowWarMachine>()
    val piles = CopyOnWriteArrayList<SnowWarPile>()
    val trees = CopyOnWriteArrayList<SnowWarTree>()
    val snowballs = CopyOnWriteArrayList<SnowWarSnowball>()
    val nextObjectId = AtomicInteger(100)

    var arenaHeightMapRows = emptyList<String>()

    var state: SnowWarStageState = SnowWarStageState.INACTIVE
    var started = false
    var countdownStarted = false
    private var countdownFuture: ScheduledFuture<*>? = null
    private var stageLoadingFuture: ScheduledFuture<*>? = null
    private var gameFuture: ScheduledFuture<*>? = null
    private var gameTickFuture: ScheduledFuture<*>? = null
    val duration: Int get() = HabboServer.habboConfig.gameConfig.snowwar.gameDurationSeconds
    private val turn = AtomicInteger(0)
    private val pendingEvents = ConcurrentLinkedQueue<Pair<Int, ISnowWarGameEvent>>()
    private val deferredEvents = ConcurrentLinkedQueue<Pair<Int, ISnowWarGameEvent>>()

    enum class DelayedEventType { HIT, STUN }
    data class DelayedCollisionEvent(
        val type: DelayedEventType,
        val target: SnowWarUser,
        val thrower: SnowWarUser,
        val ballDirection: Int
    )

    fun toLobbyData(): SnowWarLobbyData {
        return SnowWarLobbyData(
            gameId = gameId,
            levelName = levelName,
            gameType = gameType,
            fieldType = fieldType.id,
            numberOfTeams = numberOfTeams,
            maximumPlayers = maximumPlayers,
            owningPlayerName = owningPlayerName,
            levelEntryId = 0,
            players = users.values.toList()
        )
    }

    fun addPlayer(session: HabboSession) {
        val userId = session.userInformation.id
        if (users.containsKey(userId) || users.size >= maximumPlayers || started) return

        if (session.roomUser != null) {
            session.currentRoom?.userManager?.removeEntity(
                session.roomUser,
                notifyClient = true,
                kickNotification = false
            )
        }

        val team1Count = users.values.count { it.team == SnowWarTeam.BLUE }
        val team2Count = users.values.count { it.team == SnowWarTeam.RED }
        val team = if (team1Count <= team2Count) SnowWarTeam.BLUE else SnowWarTeam.RED

        val snowWarUser = SnowWarUser(session, team)
        users[userId] = snowWarUser

        session.sendHabboResponse(Outgoing.GAME_2_GAME_CREATED, toLobbyData())

        val joinedPayload = Game2UserJoinedGameData(snowWarUser, false)
        users.values.filter { it.session != session }.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_USER_JOINED_GAME, joinedPayload)
        }

        log.info(
            "[Lobby-{}] Player {} (userId={}) joined lobby on team={}, total players: {}/{}",
            gameId,
            session.userInformation.username,
            userId,
            team,
            users.size,
            maximumPlayers
        )
        checkCountdown()
    }

    fun removePlayer(session: HabboSession) {
        val userId = session.userInformation.id
        val user = users[userId]
        if (state == SnowWarStageState.STAGE_RUNNING && user != null) {
            pendingEvents.add(0 to HumanLeftGameEvent(user.objectId))
        }

        users.remove(userId)
        log.info(
            "[Lobby-{}] Player {} (userId={}) left lobby, remaining: {}",
            gameId,
            session.userInformation.username,
            userId,
            users.size
        )

        users.values.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_USER_LEFT_GAME, userId)
        }

        val minPlayers = HabboServer.habboConfig.gameConfig.snowwar.minPlayers
        if (countdownStarted && users.size < minPlayers && !started) {
            countdownStarted = false
            countdownFuture?.cancel(true)
            countdownFuture = null
            log.info(
                "[Lobby-{}] Cancelled lobby countdown: players ({}) < minPlayers ({})",
                gameId,
                users.size,
                minPlayers
            )
            users.values.forEach {
                it.session.sendHabboResponse(Outgoing.GAME_2_STOP_COUNTER)
            }
        }

        if (users.isEmpty()) {
            countdownFuture?.cancel(true)
            stageLoadingFuture?.cancel(true)
            gameFuture?.cancel(true)
            gameTickFuture?.cancel(true)
            HabboServer.habboGame.snowWarManager.removeLobby(gameId)
        }
    }

    fun checkCountdown() {
        val minPlayers = HabboServer.habboConfig.gameConfig.snowwar.minPlayers
        if (!countdownStarted && !started && users.size >= minPlayers) {
            countdownStarted = true
            val count = HabboServer.habboConfig.gameConfig.snowwar.countdownSeconds
            log.info(
                "[Lobby-{}] Starting lobby countdown: {}s (players: {} >= minPlayers: {})",
                gameId,
                count,
                users.size,
                minPlayers
            )
            users.values.forEach {
                it.session.sendHabboResponse(Outgoing.GAME_2_START_COUNTER, count)
            }
            countdownFuture = HabboServer.serverScheduledExecutor.schedule({
                startGame()
            }, count.toLong(), TimeUnit.SECONDS)
        }
    }

    fun startGame() {
        if (started || users.isEmpty()) return
        started = true
        state = SnowWarStageState.STAGE_LOADING
        log.info(
            "[Lobby-{}] startGame: state transitioned to STAGE_LOADING with {} players, fieldType={} (arenaId={})",
            gameId,
            users.size,
            fieldType,
            fieldType.id
        )

        val userList = users.values.toList()
        val lobbyData = toLobbyData()
        users.values.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_GAME_STARTED, lobbyData)
        }

        val arena = SnowWarArenaMaps.getArena(fieldType) ?: error("No arena found!")
        val arenaWidth = arena.width
        val arenaHeight = arena.height
        val arenaHeightMap = arena.heightMap
        val arenaFuseObjects = arena.fuseObjects

        log.info(
            "[Lobby-{}] Loaded arena '{}' (id={}): size={}x{}, fuseObjects={}",
            gameId,
            arena.name,
            arena.id,
            arenaWidth,
            arenaHeight,
            arenaFuseObjects.size
        )

        arenaHeightMapRows = arenaHeightMap.split("\r")

        machines.clear()
        piles.clear()
        trees.clear()
        snowballs.clear()
        nextObjectId.set(100)

        for (obj in arenaFuseObjects) {
            when {
                obj.name in listOf("snowball_machine", "s_snowball_machine") -> {
                    machines.add(SnowWarMachine(nextObjectId.getAndIncrement(), obj.x, obj.y, obj.direction, 5, obj.id))
                }

                obj.name.startsWith("snst_tree") -> {
                    trees.add(SnowWarTree(nextObjectId.getAndIncrement(), obj.x, obj.y, obj.direction, 3200, obj.id, 3))
                }

                obj.name == "snst_ballpile" -> {
                    piles.add(SnowWarPile(nextObjectId.getAndIncrement(), obj.x, obj.y, 12, obj.id))
                }
            }
        }

        userList.forEachIndexed { index, user ->
            user.setupSpawn(index, arena)
        }

        val enterArenaData = Game2EnterArenaData(
            gameType = gameType,
            fieldType = fieldType.id,
            numberOfTeams = numberOfTeams,
            players = userList,
            width = arenaWidth,
            height = arenaHeight,
            heightMap = arenaHeightMap,
            fuseObjects = arenaFuseObjects
        )
        users.values.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_ENTER_ARENA, enterArenaData)
            it.session.sendHabboResponse(Outgoing.GAME_2_STAGE_LOAD, gameType)
            it.session.sendHabboResponse(
                Outgoing.GAME_2_STAGE_STILL_LOADING,
                Game2StageStillLoadingData(percentage = 0, finishedPlayers = emptyList())
            )
        }

        // Safety fallback: after 10 seconds, start stage only if a client disconnected/lagged
        stageLoadingFuture = HabboServer.serverScheduledExecutor.schedule({
            synchronized(this) {
                if (state == SnowWarStageState.STAGE_LOADING) {
                    startStageStarting()
                }
            }
        }, 10, TimeUnit.SECONDS)
    }

    fun onPlayerLoaded(session: HabboSession) {
        val user = users[session.userInformation.id] ?: return
        user.isLoaded = true
        log.info(
            "[Lobby-{}] Player {} (userId={}) reported LOAD_STAGE_READY (100%)",
            gameId,
            session.userInformation.username,
            user.userId
        )

        val finishedPlayers = users.values.filter { it.isLoaded }.map { it.userId }
        val percentage = if (users.isNotEmpty()) (finishedPlayers.size * 100) / users.size else 100

        users.values.forEach {
            it.session.sendHabboResponse(
                Outgoing.GAME_2_STAGE_STILL_LOADING,
                Game2StageStillLoadingData(
                    percentage = percentage,
                    finishedPlayers = finishedPlayers
                )
            )
        }

        synchronized(this) {
            if (users.values.all { it.isLoaded } && state == SnowWarStageState.STAGE_LOADING) {
                log.info(
                    "[Lobby-{}] All players loaded ({}/{}), cancelling timeout fallback and launching startStageStarting",
                    gameId,
                    finishedPlayers.size,
                    users.size
                )
                stageLoadingFuture?.cancel(true)
                stageLoadingFuture = null
                startStageStarting()
            }
        }
    }

    private fun startStageStarting() {
        if (state != SnowWarStageState.STAGE_LOADING) return
        state = SnowWarStageState.STAGE_STARTING
        stageLoadingFuture?.cancel(true)
        stageLoadingFuture = null

        val stageStartingSeconds = HabboServer.habboConfig.gameConfig.snowwar.stageStartingSeconds

        // Agrupa TODOS os objetos na mesma ordem que o calculateChecksum itera:
        val initialGameObjects = mutableListOf<SnowWarGameObject>()
        initialGameObjects.addAll(machines)
        initialGameObjects.addAll(piles)
        initialGameObjects.addAll(trees)
        initialGameObjects.addAll(users.values.sortedBy { it.objectId })

        log.info(
            "[Lobby-{}] startStageStarting: countdown={}s, gameObjects={} (machines={}, piles={}, trees={}, users={})",
            gameId, stageStartingSeconds, initialGameObjects.size, machines.size, piles.size, trees.size, users.size
        )

        val stageStartingData = Game2StageStartingData(
            gameType = gameType,
            roomType = "snowwar_stage_1",
            countDown = stageStartingSeconds,
            gameObjects = initialGameObjects
        )

        users.values.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_STAGE_STARTING, stageStartingData)
        }

        HabboServer.serverScheduledExecutor.schedule({
            startStageRunning()
        }, stageStartingSeconds.toLong(), TimeUnit.SECONDS)
    }

    private fun startStageRunning() {
        if (state != SnowWarStageState.STAGE_STARTING || users.isEmpty()) return
        state = SnowWarStageState.STAGE_RUNNING
        turn.set(0)
        pendingEvents.clear()

        val duration = HabboServer.habboConfig.gameConfig.snowwar.gameDurationSeconds
        log.info(
            "[Lobby-{}] startStageRunning: match started, duration={}s, running ticks every 150ms",
            gameId,
            duration
        )

        users.values.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_STAGE_RUNNING, duration)
        }

        gameTickFuture = HabboServer.serverScheduledExecutor.scheduleAtFixedRate({
            tickGame()
        }, 150, 150, TimeUnit.MILLISECONDS)

        gameFuture = HabboServer.serverScheduledExecutor.schedule({
            endGame()
        }, duration.toLong(), TimeUnit.SECONDS)
    }

    private fun tickGame() {
        if (state != SnowWarStageState.STAGE_RUNNING || users.isEmpty()) {
            gameTickFuture?.cancel(true)
            return
        }

        val currentTurn = turn.getAndIncrement()

        val subTurns = mutableListOf<MutableList<ISnowWarGameEvent>>()
        repeat(3) { subTurns.add(mutableListOf()) }

        val eventsToApply = mutableListOf<MutableList<ISnowWarGameEvent>>()
        repeat(3) { eventsToApply.add(mutableListOf()) }

        val eventsToSend = mutableListOf<MutableList<ISnowWarGameEvent>>()
        repeat(3) { eventsToSend.add(mutableListOf()) }

        // 1. Process deferred events: aplica agora o que foi enviado no turno passado
        while (true) {
            val item = deferredEvents.poll() ?: break
            val subIndex = item.first.coerceIn(0, 2)
            eventsToApply[subIndex].add(item.second)
        }

        // 2. Process pending user events: envia agora, mas aplica na física só no próximo
        while (true) {
            val item = pendingEvents.poll() ?: break
            val subIndex = item.first.coerceIn(0, 2)
            deferredEvents.add(subIndex to item.second)
            eventsToSend[subIndex].add(item.second)
        }

        val delayedEvents = mutableListOf<DelayedCollisionEvent>()

        // Physics Simulation (3 subturns)
        for (sub in 0 until 3) {
            val subturnEvents = subTurns[sub]

            // Phase 0: Aplica as ações do servidor em sincronia com o Client!
            // Phase 0: Aplica as ações do servidor em sincronia com o Client!
            for (ev in eventsToApply[sub]) {
                when (ev) {
                    is NewMoveTargetGameEvent -> {
                        val user = users.values.find { it.objectId == ev.humanGameObjectId }
                        if (user != null) {
                            user.moveTargetX = ev.x
                            user.moveTargetY = ev.y
                        }
                    }

                    is HumanStartsToMakeASnowballGameEvent -> {
                        val user = users.values.find { it.objectId == ev.humanGameObjectId }
                        if (user != null && (user.activityState == 0 || user.activityState == 3)) {
                            user.activityState = 1 // MAKING_SNOWBALL
                            user.activityTimer = SnowWarMath.CREATING_TIMER
                            user.stopWalking()
                        }
                    }

                    is HumanThrowsSnowballAtPositionGameEvent -> {
                        val user = users.values.find { it.objectId == ev.humanGameObjectId }
                        if (user != null && user.snowBallCount > 0) {
                            user.snowBallCount--
                            user.throws.incrementAndGet()

                            val ball = SnowWarSnowball(
                                objectId = nextObjectId.getAndIncrement(),
                                thrower = user,
                                startWorldX = user.currentLocationX,
                                startWorldY = user.currentLocationY,
                                targetWorldX = ev.targetX,
                                targetWorldY = ev.targetY,
                                trajectoryRequested = ev.trajectory
                            )
                            snowballs.add(ball)
                        }
                    }

                    is HumanThrowsSnowballAtHumanGameEvent -> {
                        val user = users.values.find { it.objectId == ev.humanGameObjectId }
                        val targetUser = users.values.find { it.objectId == ev.targetHumanGameObjectId }
                        if (user != null && targetUser != null && user.snowBallCount > 0) {
                            user.snowBallCount--
                            user.throws.incrementAndGet()

                            val ball = SnowWarSnowball(
                                objectId = nextObjectId.getAndIncrement(),
                                thrower = user,
                                startWorldX = user.currentLocationX,
                                startWorldY = user.currentLocationY,
                                targetWorldX = targetUser.currentLocationX,
                                targetWorldY = targetUser.currentLocationY,
                                trajectoryRequested = ev.trajectory
                            )
                            snowballs.add(ball)
                        }
                    }

                    is MachineCreatesSnowballGameEvent -> {
                        val machine = machines.find { it.objectId == ev.snowBallMachineReference }
                        machine?.addSnowball()
                    }

                    is HumanGetsSnowballsFromMachineGameEvent -> {
                        val user = users.values.find { it.objectId == ev.humanGameObjectId }
                        val machine = machines.find { it.objectId == ev.snowBallMachineReference }
                        if (user != null && machine != null) {
                            machine.transferReservedSnowballTo(user)
                        } else {
                            val pile = piles.find { it.objectId == ev.snowBallMachineReference }
                            if (user != null && pile != null) {
                                pile.transferReservedSnowballTo(user)
                            }
                        }
                    }

                    else -> {}
                }
            }

            // Phase 1: Move all active players
            users.values.forEach { it.subturn(arenaHeightMapRows) }

            // Phase 2: Move snowballs & test collisions
            for (ball in snowballs.toList()) {
                if (!ball.alive) continue
                ball.calculateFrameMovement()
                if (checkObjectCollision(ball, delayedEvents) || ball.hasFloorCollision(arenaHeightMapRows)) {
                    ball.kill()
                    snowballs.remove(ball)
                }
            }

            // Phase 3: Process snowball machines
            for (machine in machines) {
                if (machine.processGeneratorTick()) {
                    val ev = MachineCreatesSnowballGameEvent(machine.objectId)
                    subturnEvents.add(ev)
                    // ADIA a criação para que aconteça na Fase 0 do próximo turno
                    deferredEvents.add(sub to ev)
                }
            }

            // Phase 4: Auto-collect from machine and pile pickup tiles
            for (user in users.values) {
                if (!user.tickSnowballPickupTimer()) continue
                var pickedUp = false
                for (machine in machines) {
                    if (machine.canPlayerPickup(user)) {
                        machine.reservePickup() // Reserva a bola pra mais ninguém pegar no mesmo turno
                        user.resetSnowballPickupTimer()
                        val ev = HumanGetsSnowballsFromMachineGameEvent(user.objectId, machine.objectId)
                        subturnEvents.add(ev)
                        deferredEvents.add(sub to ev) // Adia a transferência final
                        pickedUp = true
                        break
                    }
                }
                if (pickedUp) continue
                for (pile in piles) {
                    if (pile.canPlayerPickup(user)) {
                        pile.reservePickup()
                        user.resetSnowballPickupTimer()
                        val ev = HumanGetsSnowballsFromMachineGameEvent(user.objectId, pile.objectId)
                        subturnEvents.add(ev)
                        deferredEvents.add(sub to ev)
                        break
                    }
                }
            }
        }

        // Phase 5: Aplica hits e colisões (Isso não adia pois a física é autônoma no client)
        for (ev in delayedEvents) {
            val target = ev.target
            val thrower = ev.thrower
            when (ev.type) {
                DelayedEventType.HIT -> {
                    target.hitPoints = target.pendingHealth
                    thrower.score.addAndGet(1)
                    thrower.hits.incrementAndGet()
                }

                DelayedEventType.STUN -> {
                    target.stopWalking()
                    target.activityState = 2
                    target.activityTimer = SnowWarMath.STUNNED_TIMER
                    target.bodyDirection = (SnowWarMath.direction360To8(ev.ballDirection) + 4) % 8
                    thrower.score.addAndGet(5)
                    thrower.kills.incrementAndGet()
                    target.deaths.incrementAndGet()
                }
            }
        }

        // Phase 6: Checksum sempre por último!
        val cs = calculateChecksum(currentTurn)

        // Merge dos eventos automáticos com os manuais
        for (sub in 0 until 3) {
            eventsToSend[sub].addAll(subTurns[sub])
        }

        val statusData = Game2GameStatusData(
            turn = currentTurn,
            checksum = cs,
            subTurns = eventsToSend
        )

        users.values.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_GAME_STATUS, statusData)
        }
    }

    private fun checkObjectCollision(
        ball: SnowWarSnowball,
        delayedEvents: MutableList<DelayedCollisionEvent>
    ): Boolean {
        val collisionTiles = getCollisionTiles(ball)
        for (tile in collisionTiles) {
            for (tree in trees) {
                if (tree.x == tile.first && tree.y == tile.second && tree.testCollision(ball)) {
                    tree.hit()
                    return true
                }
            }
            for (machine in machines) {
                if (machine.x == tile.first && machine.y == tile.second && machine.testCollision(ball)) {
                    return true
                }
            }
            for (user in users.values) {
                if (user.currentTileX != tile.first || user.currentTileY != tile.second || !user.testCollision(ball)) {
                    continue
                }
                if (user.team != ball.thrower.team) {
                    applyAvatarHit(user, ball, delayedEvents)
                }
                return true
            }
        }
        return false
    }

    private fun applyAvatarHit(
        player: SnowWarUser,
        ball: SnowWarSnowball,
        delayedEvents: MutableList<DelayedCollisionEvent>
    ) {
        if (player.pendingHealth > 1) {
            player.pendingHealth -= 1
            delayedEvents.add(DelayedCollisionEvent(DelayedEventType.HIT, player, ball.thrower, ball.direction))
        } else if (player.pendingHealth == 1 && !player.pendingStun) {
            player.pendingHealth = 0
            player.pendingStun = true
            delayedEvents.add(DelayedCollisionEvent(DelayedEventType.HIT, player, ball.thrower, ball.direction))
            delayedEvents.add(DelayedCollisionEvent(DelayedEventType.STUN, player, ball.thrower, ball.direction))
        }
    }

    private fun getCollisionTiles(ball: SnowWarSnowball): List<Pair<Int, Int>> {
        val currentX = SnowWarMath.worldToTile(ball.locH)
        val currentY = SnowWarMath.worldToTile(ball.locV)
        val direction = SnowWarMath.direction360To8(ball.direction)
        val offsetX = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
        val offsetY = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)
        val left = (direction - 1 + 8) % 8
        val right = (direction + 1) % 8
        return listOf(
            currentX to currentY,
            (currentX + offsetX[direction]) to (currentY + offsetY[direction]),
            (currentX + offsetX[left]) to (currentY + offsetY[left]),
            (currentX + offsetX[right]) to (currentY + offsetY[right])
        )
    }

    fun calculateChecksum(turnNumber: Int): Int {
        var checksum = QuickRandom.iterateSeed(turnNumber)

        for (machine in machines) {
            if (machine.isAlive()) {
                checksum += machine.getChecksumContribution()
            }
        }

        for (pile in piles) {
            if (pile.isAlive()) {
                checksum += pile.getChecksumContribution()
            }
        }

        for (tree in trees) {
            if (tree.isAlive()) {
                checksum += tree.getChecksumContribution()
            }
        }

        // Jogadores ordenados pelo objectId
        for (user in users.values.sortedBy { it.objectId }) {
            if (user.isAlive()) {
                checksum += user.getChecksumContribution()
            }
        }

        for (ball in snowballs) {
            if (ball.isAlive()) {
                checksum += ball.getChecksumContribution()
            }
        }

        if (turnNumber == 0) {
            log.info(
                "[Lobby-{}] Checksum Turn 0: serverChecksum={}, objects: machines={}, piles={}, trees={}, users={}, balls={}",
                gameId, checksum, machines.size, piles.size, trees.size, users.size, snowballs.size
            )
        }

        return checksum
    }

    fun onFullStatusRequest(session: HabboSession, requestedTurn: Int) {
        val currentTurn = turn.get()
        log.info("[SnowWar] - Client asked for full status at turn $currentTurn")
        val elapsedSeconds = (currentTurn * 150) / 1000
        val remaining = (duration - elapsedSeconds).coerceAtLeast(0)
        val cs = calculateChecksum(currentTurn)
        val statusData = Game2GameStatusData(
            turn = currentTurn,
            checksum = cs,
            subTurns = listOf(emptyList(), emptyList(), emptyList())
        )
        val allObjects = mutableListOf<SnowWarGameObject>()
        allObjects.addAll(machines)
        allObjects.addAll(piles)
        allObjects.addAll(trees)
        allObjects.addAll(users.values)
        allObjects.addAll(snowballs)

        val fullData = Game2FullGameStatusData(
            remainingTimeSeconds = remaining,
            durationInSeconds = duration,
            objects = allObjects,
            numberOfTeams = numberOfTeams,
            status = statusData
        )
        session.sendHabboResponse(Outgoing.GAME_2_FULL_GAME_STATUS, fullData)
    }

    fun onUserMove(session: HabboSession, targetX: Int, targetY: Int, turnNumber: Int, subTurn: Int) {
        val user = users[session.userInformation.id] ?: return
        if (state == SnowWarStageState.STAGE_RUNNING) {
            log.info(
                "[SnowWar] onUserMove: userId={} objectId={} targetX={} targetY={} (tileX={}/tileY={}) turn={} subTurn={}",
                user.userId,
                user.objectId,
                targetX,
                targetY,
                targetX / 3200,
                targetY / 3200,
                turnNumber,
                subTurn
            )
            val evt = NewMoveTargetGameEvent(user.objectId, targetX, targetY)
            pendingEvents.add(subTurn.coerceIn(0, 2) to evt)
            log.info(
                "[SnowWar] onUserMove: userId={} objectId={} targetX={} targetY={} (tileX={}/tileY={}) senderTurn={} subTurn={} eventId={}",
                session.userInformation.id,
                user.objectId,
                targetX,
                targetY,
                targetX.div(3200),
                targetY.div(3200),
                turnNumber,
                subTurn,
                evt.hashCode()
            )
        }
    }

    fun onMakeSnowball(session: HabboSession, turnNumber: Int, subTurn: Int) {
        val user = users[session.userInformation.id] ?: return
        if (state == SnowWarStageState.STAGE_RUNNING && user.snowBallCount < SnowWarMath.MAX_SNOWBALLS &&
            (user.activityState == 0 || user.activityState == 3)
        ) {
            pendingEvents.add(subTurn.coerceIn(0, 2) to HumanStartsToMakeASnowballGameEvent(user.objectId))
        }
    }

    fun onThrowSnowballAtPosition(
        session: HabboSession,
        targetX: Int,
        targetY: Int,
        trajectory: Int,
        turn: Int,
        sub: Int
    ) {
        val user = users[session.userInformation.id] ?: return
        if (state != SnowWarStageState.STAGE_RUNNING || user.snowBallCount <= 0) return

        // NÃO crie a bola aqui! Apenas enfileire o evento:
        pendingEvents.add(
            sub.coerceIn(0, 2) to HumanThrowsSnowballAtPositionGameEvent(
                user.objectId,
                targetX,
                targetY,
                trajectory
            )
        )
    }

    fun onThrowSnowballAtHuman(
        session: HabboSession,
        targetHumanGameObjectId: Int,
        trajectory: Int,
        turn: Int,
        sub: Int
    ) {
        val user = users[session.userInformation.id] ?: return
        if (state != SnowWarStageState.STAGE_RUNNING || user.snowBallCount <= 0) return

        // NÃO crie a bola aqui! Apenas enfileire o evento:
        pendingEvents.add(
            sub.coerceIn(0, 2) to HumanThrowsSnowballAtHumanGameEvent(
                user.objectId,
                targetHumanGameObjectId,
                trajectory
            )
        )
    }

    fun endGame() {
        if (state == SnowWarStageState.GAME_OVER) return
        state = SnowWarStageState.GAME_OVER

        gameTickFuture?.cancel(true)
        gameFuture?.cancel(true)

        users.values.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_STAGE_ENDING, 5)
        }

        val team1Users = users.values.filter { it.team == SnowWarTeam.BLUE }
        val team2Users = users.values.filter { it.team == SnowWarTeam.RED }
        val team1TotalScore = team1Users.sumOf { it.score.get() }
        val team2TotalScore = team2Users.sumOf { it.score.get() }

        users.values.forEach { user ->
            val session = user.session
            val uid = user.userId
            val scoreVal = user.score.get()
            val hitsVal = user.hits.get()
            val throwsVal = user.throws.get()

            // Update stats in DB
            val stats = HabboServer.habboGame.snowWarManager.getPlayerStats(uid)
            stats.totalScore += scoreVal
            stats.weeklyScore += scoreVal
            stats.gamesPlayed += 1
            stats.weeklyGamesPlayed += 1
            stats.skillLevel = (stats.totalScore / 100).coerceAtLeast(1)
            HabboServer.habboGame.snowWarManager.savePlayerStats(stats)

            // Progress Achievements (Official Habbo texts)
            if (scoreVal > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    session,
                    "ACH_SnowWarTotalScore",
                    scoreVal,
                    accumulate = true
                )
            }
            if (hitsVal > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    session,
                    "ACH_SnowStormHit",
                    hitsVal,
                    accumulate = true
                )
            }
            if (throwsVal > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    session,
                    "ACH_SnowStormThrow",
                    throwsVal,
                    accumulate = true
                )
            }
        }

        val winningTeam = when {
            team1TotalScore > team2TotalScore -> SnowWarTeam.BLUE.id
            team2TotalScore > team1TotalScore -> SnowWarTeam.RED.id
            else -> 0
        }
        val resultType = if (winningTeam == 0) SnowWarResultType.TIE.id else SnowWarResultType.WINNER.id

        // Award winner achievement (Snowmaster)
        if (winningTeam != 0) {
            users.values.filter { it.team.id == winningTeam }.forEach { winnerUser ->
                HabboServer.habboGame.achievementManager.progress(
                    winnerUser.session,
                    "ACH_SnowStormWin",
                    1,
                    accumulate = true
                )
            }
        }

        val mostKillsUser = users.values.maxByOrNull { it.kills.get() }
        val mostHitsUser = users.values.maxByOrNull { it.hits.get() }

        // Award MVP achievement
        if (mostHitsUser != null && mostHitsUser.hits.get() > 0) {
            HabboServer.habboGame.achievementManager.progress(
                mostHitsUser.session,
                "ACH_SnowWarWeeklyBest",
                1,
                accumulate = true
            )
        }

        val endingData = Game2GameEndingData(
            timeToNextState = 10,
            isDeathMatch = false,
            resultType = resultType,
            winnerId = winningTeam,
            teams = listOf(
                Game2TeamScoreData(
                    teamReference = SnowWarTeam.BLUE.id,
                    score = team1TotalScore,
                    players = team1Users
                ),
                Game2TeamScoreData(
                    teamReference = SnowWarTeam.RED.id,
                    score = team2TotalScore,
                    players = team2Users
                )
            ),
            playerWithMostKills = mostKillsUser?.userId ?: 0,
            playerWithMostHits = mostHitsUser?.userId ?: 0
        )

        users.values.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_GAME_ENDING, endingData)
        }
    }

    fun chat(session: HabboSession, message: String) {
        if (!users.containsKey(session.userInformation.id)) return
        val chatPayload = Game2GameChatFromPlayerData(session.userInformation.id, message)
        users.values.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_GAME_CHAT_FROM_PLAYER, chatPayload)
        }
    }
}
