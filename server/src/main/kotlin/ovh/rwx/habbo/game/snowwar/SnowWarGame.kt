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
import ovh.rwx.habbo.game.snowwar.enums.*
import ovh.rwx.habbo.game.snowwar.objects.*
import ovh.rwx.habbo.game.snowwar.utils.QuickRandom
import ovh.rwx.habbo.game.snowwar.utils.SnowWarMath
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class SnowWarGame(
    val gameId: Int,
    val levelName: String = "snowwar_stage_1",
    val gameType: Int = 0,
    val fieldType: SnowWarFieldType = SnowWarFieldType.random(),
    val numberOfTeams: Int = 2,
    val users: ConcurrentHashMap<Int, SnowWarUser>,
    val lobbyDataProvider: () -> SnowWarLobbyData,
    val onGameOver: () -> Unit = {}
) {
    private val log = LoggerFactory.getLogger("SnowWarGame-$gameId")

    val blockedTiles = HashSet<Pair<Int, Int>>()
    val machines = CopyOnWriteArrayList<SnowWarMachine>()
    val piles = CopyOnWriteArrayList<SnowWarPile>()
    val trees = CopyOnWriteArrayList<SnowWarTree>()
    val snowballs = CopyOnWriteArrayList<SnowWarSnowball>()
    val nextObjectId = AtomicInteger(100)

    var arenaHeightMapRows = emptyList<String>()

    var state: SnowWarStageState = SnowWarStageState.INACTIVE
        private set

    private var stageLoadingFuture: ScheduledFuture<*>? = null
    private var gameFuture: ScheduledFuture<*>? = null
    private var gameTickFuture: ScheduledFuture<*>? = null
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

    fun start() {
        if (state != SnowWarStageState.INACTIVE || users.isEmpty()) return
        state = SnowWarStageState.STAGE_LOADING

        log.info(
            "[Game-{}] start: state transitioned to STAGE_LOADING with {} players, fieldType={} (arenaId={})",
            gameId,
            users.size,
            fieldType,
            fieldType.id
        )

        val userList = users.values.toList()
        val lobbyData = lobbyDataProvider()
        users.values.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_GAME_STARTED, lobbyData)
        }

        val arena = SnowWarArenaMaps.getArena(fieldType) ?: error("No arena found!")
        val arenaWidth = arena.width
        val arenaHeight = arena.height
        val arenaHeightMap = arena.heightMap
        val arenaFuseObjects = arena.fuseObjects

        log.info(
            "[Game-{}] Loaded arena '{}' (id={}): size={}x{}, fuseObjects={}",
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
        blockedTiles.clear()
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

        for (tree in trees) {
            blockedTiles.add(tree.x to tree.y)
        }
        for (machine in machines) {
            blockedTiles.add(machine.x to machine.y)
        }
        for (pile in piles) {
            blockedTiles.add(pile.x to pile.y)
        }
        for (obj in arenaFuseObjects) {
            val furnishing = obj.furnishing
            if (furnishing != null && !furnishing.walkable) {
                var w = furnishing.width
                var l = furnishing.length
                if (obj.direction == 2 || obj.direction == 6) {
                    val tmp = w
                    w = l
                    l = tmp
                }
                for (dx in 0 until w) {
                    for (dy in 0 until l) {
                        blockedTiles.add((obj.x + dx) to (obj.y + dy))
                    }
                }
            }
        }

        val blueSpawns = (arena.blueSpawns.takeIf { it.isNotEmpty() } ?: listOf(22 to 9)).shuffled()
        val redSpawns = (arena.redSpawns.takeIf { it.isNotEmpty() } ?: listOf(30 to 43)).shuffled()

        var blueIndex = 0
        var redIndex = 0

        userList.forEachIndexed { index, user ->
            val spawnTile = if (user.team == SnowWarTeam.BLUE) {
                blueSpawns[blueIndex++ % blueSpawns.size]
            } else {
                redSpawns[redIndex++ % redSpawns.size]
            }
            user.setupSpawn(index, spawnTile, arenaWidth, arenaHeight)
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

        // Safety fallback: after 10 seconds, start stage if a client disconnected/lagged
        stageLoadingFuture = HabboServer.serverScheduledExecutor.schedule({
            synchronized(this) {
                if (state == SnowWarStageState.STAGE_LOADING) {
                    startStageStarting()
                }
            }
        }, 10, TimeUnit.SECONDS)
    }

    fun onPlayerLoaded(session: HabboSession) {
        if (state != SnowWarStageState.STAGE_LOADING) return
        val user = users[session.userInformation.id] ?: return
        user.isLoaded = true
        log.info(
            "[Game-{}] Player {} (userId={}) reported LOAD_STAGE_READY (100%)",
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
                    "[Game-{}] All players loaded ({}/{}), cancelling timeout fallback and launching startStageStarting",
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

        val initialGameObjects = mutableListOf<SnowWarGameObject>()
        initialGameObjects.addAll(machines)
        initialGameObjects.addAll(piles)
        initialGameObjects.addAll(trees)
        initialGameObjects.addAll(users.values.sortedBy { it.objectId })

        log.info(
            "[Game-{}] startStageStarting: countdown={}s, gameObjects={} (machines={}, piles={}, trees={}, users={})",
            gameId, stageStartingSeconds, initialGameObjects.size, machines.size, piles.size, trees.size, users.size
        )

        val stageStartingData = Game2StageStartingData(
            gameType = gameType,
            roomType = levelName,
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
            "[Game-{}] startStageRunning: match started, duration={}s, running ticks every 150ms",
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

        // 2. Process pending user events: envia agora, mas aplica na física só no próximo turno
        while (true) {
            val item = pendingEvents.poll() ?: break
            val subIndex = item.first.coerceIn(0, 2)
            deferredEvents.add(subIndex to item.second)
            eventsToSend[subIndex].add(item.second)
        }

        val delayedEvents = mutableListOf<DelayedCollisionEvent>()

        // Simulação física em 3 subturnos
        for (sub in 0 until 3) {
            val subturnEvents = subTurns[sub]

            // Phase 0: Aplica ações sincronizadas no cliente
            for (ev in eventsToApply[sub]) {
                ev.apply(this)
            }

            // Phase 1: Move players
            for (user in users.values) {
                user.subturn { tx, ty -> isTileWalkable(tx, ty, user) }
            }

            // Phase 2: Move snowballs & test collisions
            for (ball in snowballs.toList()) {
                if (!ball.alive) continue
                ball.calculateFrameMovement()
                if (checkObjectCollision(ball, delayedEvents) || ball.hasFloorCollision(arenaHeightMapRows)) {
                    ball.kill()
                    snowballs.remove(ball)
                }
            }

            // Phase 3: Generator ticks das máquinas
            for (machine in machines) {
                if (machine.processGeneratorTick()) {
                    deferredEvents.add(sub to MachineCreatesSnowballGameEvent(machine.objectId))
                    eventsToSend[sub].add(MachineCreatesSnowballGameEvent(machine.objectId))
                }
            }

            // Phase 4: Coleta de bolas em máquinas e pilhas
            for (user in users.values) {
                if (!user.tickSnowballPickupTimer()) continue

                val targetMachine = machines.find { it.canPlayerPickup(user) }
                if (targetMachine != null) {
                    targetMachine.reservePickup()
                    deferredEvents.add(
                        sub to HumanGetsSnowballsFromMachineGameEvent(
                            user.objectId,
                            targetMachine.objectId
                        )
                    )
                    eventsToSend[sub].add(HumanGetsSnowballsFromMachineGameEvent(user.objectId, targetMachine.objectId))
                    continue
                }

                val targetPile = piles.find { it.canPlayerPickup(user) }
                if (targetPile != null) {
                    targetPile.reservePickup()
                    deferredEvents.add(
                        sub to HumanGetsSnowballsFromMachineGameEvent(
                            user.objectId,
                            targetPile.objectId
                        )
                    )
                    eventsToSend[sub].add(HumanGetsSnowballsFromMachineGameEvent(user.objectId, targetPile.objectId))
                }
            }
        }

        // Phase 5: Aplicação de dano/atordoamento retardado
        if (delayedEvents.isNotEmpty()) {
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
                        target.activityState = SnowWarActivityState.STUNNED
                        target.activityTimer = SnowWarMath.STUNNED_TIMER
                        target.bodyDirection = (SnowWarMath.direction360To8(ev.ballDirection) + 4) % 8
                        thrower.score.addAndGet(5)
                        thrower.kills.incrementAndGet()
                        target.deaths.incrementAndGet()
                    }
                }
            }
        }

        // Phase 6: Checksum sempre por último!
        val cs = calculateChecksum(currentTurn)

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
            delayedEvents.add(
                DelayedCollisionEvent(
                    DelayedEventType.HIT,
                    player,
                    ball.thrower,
                    ball.direction
                )
            )
        } else {
            player.pendingHealth = 0
            player.pendingStun = true
            delayedEvents.add(
                DelayedCollisionEvent(
                    DelayedEventType.STUN,
                    player,
                    ball.thrower,
                    ball.direction
                )
            )
        }
    }

    fun isTileWalkable(tileX: Int, tileY: Int, movingUser: SnowWarUser): Boolean {
        if (tileY !in 0 until arenaHeightMapRows.size) return false
        val row = arenaHeightMapRows[tileY]
        if (tileX !in 0 until row.length) return false
        val c = row[tileX]
        if (c == 'x' || c == 'X') return false
        if ((tileX to tileY) in blockedTiles) return false
        for (u in users.values) {
            if (u.objectId == movingUser.objectId || !u.isAlive()) continue
            val occX = if (u.nextTileX != -1) u.nextTileX else u.currentTileX
            val occY = if (u.nextTileY != -1) u.nextTileY else u.currentTileY
            if (occX == tileX && occY == tileY) return false
        }
        return true
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

        for (user in users.values.sortedBy { it.objectId }) {
            if (user.isAlive()) {
                checksum += user.getChecksumContribution()
            }
        }

        for (ball in snowballs) {
            if (ball.alive) {
                checksum += ball.getChecksumContribution()
            }
        }

        if (turnNumber == 0) {
            log.info(
                "[Game-{}] Checksum Turn 0: serverChecksum={}, objects: machines={}, piles={}, trees={}, users={}, balls={}",
                gameId, checksum, machines.size, piles.size, trees.size, users.size, snowballs.size
            )
        }

        return checksum
    }

    val duration: Int get() = HabboServer.habboConfig.gameConfig.snowwar.gameDurationSeconds

    fun onFullStatusRequest(session: HabboSession, requestedTurn: Int) {
        val currentTurn = turn.get()
        log.info("[Game-{}] Client asked for full status at turn {}", gameId, currentTurn)
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
        allObjects.addAll(users.values.sortedBy { it.objectId })
        allObjects.addAll(snowballs.filter { it.alive })

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
        if (state != SnowWarStageState.STAGE_RUNNING) return

        log.info(
            "[Game-{}] onUserMove: userId={} objectId={} targetX={} targetY={} (tileX={}/tileY={}) turn={} subTurn={}",
            gameId,
            user.userId,
            user.objectId,
            targetX,
            targetY,
            targetX / 3200,
            targetY / 3200,
            turnNumber,
            subTurn
        )
        pendingEvents.add(subTurn.coerceIn(0, 2) to NewMoveTargetGameEvent(user.objectId, targetX, targetY))
    }

    fun onMakeSnowball(session: HabboSession, turnNumber: Int, subTurn: Int) {
        val user = users[session.userInformation.id] ?: return
        if (state == SnowWarStageState.STAGE_RUNNING && user.snowBallCount < SnowWarMath.MAX_SNOWBALLS && user.canMove()) {
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
        if (state != SnowWarStageState.STAGE_RUNNING || user.snowBallCount <= 0 || !user.canMove()) return

        val ballId = nextObjectId.getAndIncrement()
        val subIndex = sub.coerceIn(0, 2)
        pendingEvents.add(
            subIndex to HumanThrowsSnowballAtPositionGameEvent(
                user.objectId,
                targetX,
                targetY,
                trajectory
            )
        )
        pendingEvents.add(
            subIndex to CreateSnowballGameEvent(
                ballId,
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
        if (state != SnowWarStageState.STAGE_RUNNING || user.snowBallCount <= 0 || !user.canMove()) return

        val targetUser = users.values.find { it.objectId == targetHumanGameObjectId } ?: return
        val ballId = nextObjectId.getAndIncrement()
        val subIndex = sub.coerceIn(0, 2)
        pendingEvents.add(
            subIndex to HumanThrowsSnowballAtHumanGameEvent(
                user.objectId,
                targetHumanGameObjectId,
                trajectory
            )
        )
        pendingEvents.add(
            subIndex to CreateSnowballGameEvent(
                ballId,
                user.objectId,
                targetUser.currentLocationX,
                targetUser.currentLocationY,
                trajectory
            )
        )
    }

    fun removePlayer(session: HabboSession) {
        val userId = session.userInformation.id
        val user = users[userId]
        if (state == SnowWarStageState.STAGE_RUNNING && user != null) {
            pendingEvents.add(0 to HumanLeftGameEvent(user.objectId))
        }

        users.remove(userId)
        users.values.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_USER_LEFT_GAME, userId)
        }

        if (users.isEmpty()) {
            dispose()
            onGameOver()
        }
    }

    fun endGame() {
        if (state == SnowWarStageState.GAME_OVER) return
        state = SnowWarStageState.GAME_OVER

        dispose()

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

            val stats = HabboServer.habboGame.snowWarManager.getPlayerStats(uid)
            stats.totalScore += scoreVal
            stats.weeklyScore += scoreVal
            stats.gamesPlayed += 1
            stats.weeklyGamesPlayed += 1
            stats.skillLevel = (stats.totalScore / 100).coerceAtLeast(1)
            HabboServer.habboGame.snowWarManager.savePlayerStats(stats)

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

        onGameOver()
    }

    fun chat(session: HabboSession, message: String) {
        if (!users.containsKey(session.userInformation.id)) return
        val chatPayload = Game2GameChatFromPlayerData(session.userInformation.id, message)
        users.values.forEach {
            it.session.sendHabboResponse(Outgoing.GAME_2_GAME_CHAT_FROM_PLAYER, chatPayload)
        }
    }

    fun dispose() {
        stageLoadingFuture?.cancel(true)
        stageLoadingFuture = null
        gameFuture?.cancel(true)
        gameFuture = null
        gameTickFuture?.cancel(true)
        gameTickFuture = null
    }
}
