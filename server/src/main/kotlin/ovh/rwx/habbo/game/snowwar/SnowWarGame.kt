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
import ovh.rwx.habbo.game.snowwar.bot.SnowWarBotAI
import ovh.rwx.habbo.game.snowwar.enums.SnowWarFieldType
import ovh.rwx.habbo.game.snowwar.enums.SnowWarResultType
import ovh.rwx.habbo.game.snowwar.enums.SnowWarStageState
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTeam
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTrajectory
import ovh.rwx.habbo.game.snowwar.objects.SnowWarGameObject
import ovh.rwx.habbo.game.snowwar.utils.SnowWarMath
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Adapter do ciclo de vida de uma partida SnowWar: máquina de estados, relógio
 * (HabboServer.serverScheduledExecutor), config, broadcast de pacotes e estatísticas.
 * A simulação determinística (física, eventos, checksum) vive no [SnowWarArena] —
 * testes de replay headless atravessam o seam da arena, não este adapter.
 */
class SnowWarGame(
    val gameId: Int,
    val levelName: String = "snowwar_stage_1",
    val gameType: Int = 0,
    val fieldType: SnowWarFieldType = SnowWarFieldType.random(),
    val numberOfTeams: Int = 2,
    users: ConcurrentHashMap<Int, SnowWarUser>,
    val lobbyDataProvider: () -> SnowWarLobbyData,
    val onGameOver: () -> Unit = {}
) {
    private val log = LoggerFactory.getLogger("SnowWarGame-$gameId")

    val arena = SnowWarArena(users)
    private val botAI = SnowWarBotAI(arena)

    init {
        arena.onBeforeTick = { botAI.processTurn(it) }
    }

    fun sendHabboResponse(header: Outgoing, vararg params: Any) {
        arena.users.values.forEach { it.session?.sendHabboResponse(header, *params) }
    }

    var state: SnowWarStageState = SnowWarStageState.INACTIVE
        private set

    private var stageLoadingFuture: ScheduledFuture<*>? = null
    private var gameFuture: ScheduledFuture<*>? = null
    private var gameTickFuture: ScheduledFuture<*>? = null

    val duration: Int get() = HabboServer.habboConfig.gameConfig.snowwar.gameDurationSeconds

    fun start() {
        if (state != SnowWarStageState.INACTIVE || arena.users.isEmpty()) return
        state = SnowWarStageState.STAGE_LOADING

        log.info(
            "[Game-{}] start: state transitioned to STAGE_LOADING with {} players, fieldType={} (arenaId={})",
            gameId,
            arena.users.size,
            fieldType,
            fieldType.id
        )

        val lobbyData = lobbyDataProvider()
        sendHabboResponse(Outgoing.GAME_2_GAME_STARTED, lobbyData)

        val arenaMap = SnowWarArenaMaps.getArena(fieldType) ?: error("No arena found!")

        log.info(
            "[Game-{}] Loaded arena '{}' (id={}): size={}x{}, fuseObjects={}",
            gameId,
            arenaMap.name,
            arenaMap.id,
            arenaMap.width,
            arenaMap.height,
            arenaMap.fuseObjects.size
        )

        arena.setupField(arenaMap)

        val enterArenaData = Game2EnterArenaData(
            gameType = gameType,
            fieldType = fieldType.id,
            numberOfTeams = numberOfTeams,
            players = arena.users.values.toList(),
            width = arenaMap.width,
            height = arenaMap.height,
            heightMap = arenaMap.heightMap,
            fuseObjects = arenaMap.fuseObjects
        )
        sendHabboResponse(Outgoing.GAME_2_ENTER_ARENA, enterArenaData)
        sendHabboResponse(Outgoing.GAME_2_STAGE_LOAD, gameType)
        sendHabboResponse(
            Outgoing.GAME_2_STAGE_STILL_LOADING,
            Game2StageStillLoadingData(percentage = 0, finishedPlayers = emptyList())
        )

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
        val user = arena.users[session.userInformation.id] ?: return
        user.isLoaded = true
        log.info(
            "[Game-{}] Player {} (userId={}) reported LOAD_STAGE_READY (100%)",
            gameId,
            session.userInformation.username,
            user.userId
        )

        val finishedPlayers = arena.users.values.filter { it.isLoaded }.map { it.userId }
        val percentage = if (arena.users.isNotEmpty()) (finishedPlayers.size * 100) / arena.users.size else 100

        sendHabboResponse(
            Outgoing.GAME_2_STAGE_STILL_LOADING,
            Game2StageStillLoadingData(
                percentage = percentage,
                finishedPlayers = finishedPlayers
            )
        )

        synchronized(this) {
            if (arena.users.values.all { it.isLoaded } && state == SnowWarStageState.STAGE_LOADING) {
                log.info(
                    "[Game-{}] All players loaded ({}/{}), cancelling timeout fallback and launching startStageStarting",
                    gameId,
                    finishedPlayers.size,
                    arena.users.size
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
        initialGameObjects.addAll(arena.machines)
        initialGameObjects.addAll(arena.piles)
        initialGameObjects.addAll(arena.trees)
        initialGameObjects.addAll(arena.users.values.sortedBy { it.objectId })

        log.info(
            "[Game-{}] startStageStarting: countdown={}s, gameObjects={} (machines={}, piles={}, trees={}, users={})",
            gameId, stageStartingSeconds, initialGameObjects.size, arena.machines.size, arena.piles.size, arena.trees.size, arena.users.size
        )

        val stageStartingData = Game2StageStartingData(
            gameType = gameType,
            roomType = levelName,
            countDown = stageStartingSeconds,
            gameObjects = initialGameObjects
        )

        sendHabboResponse(Outgoing.GAME_2_STAGE_STARTING, stageStartingData)

        HabboServer.serverScheduledExecutor.schedule({
            startStageRunning()
        }, stageStartingSeconds.toLong(), TimeUnit.SECONDS)
    }

    private fun startStageRunning() {
        if (state != SnowWarStageState.STAGE_STARTING || arena.users.isEmpty()) return
        state = SnowWarStageState.STAGE_RUNNING
        val checksumTurn0 = arena.resetTurns()

        log.info(
            "[Game-{}] Checksum Turn 0: serverChecksum={}, objects: machines={}, piles={}, trees={}, users={}, balls={}",
            gameId, checksumTurn0, arena.machines.size, arena.piles.size, arena.trees.size, arena.users.size, arena.snowballs.size
        )

        val duration = HabboServer.habboConfig.gameConfig.snowwar.gameDurationSeconds
        log.info(
            "[Game-{}] startStageRunning: match started, duration={}s, running ticks every 150ms",
            gameId,
            duration
        )

        sendHabboResponse(Outgoing.GAME_2_STAGE_RUNNING, duration)

        gameTickFuture = HabboServer.serverScheduledExecutor.scheduleAtFixedRate({
            tickAndBroadcast()
        }, 150, 150, TimeUnit.MILLISECONDS)

        gameFuture = HabboServer.serverScheduledExecutor.schedule({
            endGame()
        }, duration.toLong(), TimeUnit.SECONDS)
    }

    private fun tickAndBroadcast() {
        if (state != SnowWarStageState.STAGE_RUNNING) {
            gameTickFuture?.cancel(true)
            return
        }

        val statusData = arena.tick() ?: run {
            gameTickFuture?.cancel(true)
            return
        }

        sendHabboResponse(Outgoing.GAME_2_GAME_STATUS, statusData)
    }

    fun onFullStatusRequest(session: HabboSession, requestedTurn: Int) {
        val fullData = arena.fullStatusData(duration, numberOfTeams)

        log.info(
            "[Game-{}] Client asked for full status at turn {} (reason={})",
            gameId,
            arena.lastCompletedTurn,
            requestedTurn
        )

        session.sendHabboResponse(Outgoing.GAME_2_FULL_GAME_STATUS, fullData)
    }

    fun onUserMove(session: HabboSession, targetX: Int, targetY: Int, turnNumber: Int, subTurn: Int) {
        val user = arena.users[session.userInformation.id] ?: return
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
        arena.queueEvent(subTurn, NewMoveTargetGameEvent(user.objectId, targetX, targetY))
    }

    fun onMakeSnowball(session: HabboSession, turnNumber: Int, subTurn: Int) {
        val user = arena.users[session.userInformation.id] ?: return
        if (state == SnowWarStageState.STAGE_RUNNING && user.snowBallCount < SnowWarMath.MAX_SNOWBALLS && user.canMove()) {
            arena.queueEvent(subTurn, HumanStartsToMakeASnowballGameEvent(user.objectId))
        }
    }

    fun onThrowSnowballAtPosition(
        session: HabboSession,
        targetX: Int,
        targetY: Int,
        trajectory: SnowWarTrajectory,
        turn: Int,
        sub: Int
    ) {
        val user = arena.users[session.userInformation.id] ?: return
        if (state != SnowWarStageState.STAGE_RUNNING || user.snowBallCount <= 0 || !user.canMove()) return

        val ballId = arena.nextObjectId.getAndIncrement()
        arena.queueEvent(
            sub,
            HumanThrowsSnowballAtPositionGameEvent(
                user.objectId,
                targetX,
                targetY,
                trajectory
            )
        )
        arena.queueEvent(
            sub,
            CreateSnowballGameEvent(
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
        trajectory: SnowWarTrajectory,
        turn: Int,
        sub: Int
    ) {
        val user = arena.users[session.userInformation.id] ?: return
        if (state != SnowWarStageState.STAGE_RUNNING || user.snowBallCount <= 0 || !user.canMove()) return

        val targetUser = arena.users.values.find { it.objectId == targetHumanGameObjectId } ?: return
        val ballId = arena.nextObjectId.getAndIncrement()
        arena.queueEvent(
            sub,
            HumanThrowsSnowballAtHumanGameEvent(
                user.objectId,
                targetHumanGameObjectId,
                trajectory
            )
        )
        arena.queueEvent(
            sub,
            CreateSnowballGameEvent(
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
        val user = arena.users[userId]
        if (state == SnowWarStageState.STAGE_RUNNING && user != null) {
            arena.queueEvent(0, HumanLeftGameEvent(user.objectId))
        }

        arena.users.remove(userId)
        sendHabboResponse(Outgoing.GAME_2_USER_LEFT_GAME, userId)

        val realPlayers = arena.users.values.count { it.session != null }
        if (arena.users.isEmpty() || realPlayers == 0) {
            dispose()
            onGameOver()
        }
    }

    fun endGame() {
        if (state == SnowWarStageState.GAME_OVER) return
        state = SnowWarStageState.GAME_OVER

        dispose()

        sendHabboResponse(Outgoing.GAME_2_STAGE_ENDING, 5)

        val team1Users = arena.users.values.filter { it.team == SnowWarTeam.BLUE }
        val team2Users = arena.users.values.filter { it.team == SnowWarTeam.RED }
        val team1TotalScore = team1Users.sumOf { it.score.get() }
        val team2TotalScore = team2Users.sumOf { it.score.get() }

        arena.users.values.filter { it.session != null }.forEach { user ->
            val session = user.session ?: return@forEach
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
            val treeHitsVal = user.treeHits.get()
            if (treeHitsVal > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    session,
                    "ACH_SnowStormThrow",
                    treeHitsVal,
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
            arena.users.values.filter { it.team.id == winningTeam && it.session != null }.forEach { winnerUser ->
                winnerUser.session?.let { session ->
                    HabboServer.habboGame.achievementManager.progress(
                        session,
                        "ACH_SnowStormWin",
                        1,
                        accumulate = true
                    )
                }
            }
        }

        val mostKillsUser = arena.users.values.maxByOrNull { it.kills.get() }
        val mostHitsUser = arena.users.values.maxByOrNull { it.hits.get() }
        val mostHitsRealUser = arena.users.values.filter { it.session != null }.maxByOrNull { it.hits.get() }

        if (mostHitsRealUser != null && mostHitsRealUser.hits.get() > 0) {
            mostHitsRealUser.session?.let { session ->
                HabboServer.habboGame.achievementManager.progress(
                    session,
                    "ACH_SnowWarWeeklyBest",
                    1,
                    accumulate = true
                )
            }
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

        sendHabboResponse(Outgoing.GAME_2_GAME_ENDING, endingData)

        onGameOver()
    }

    fun chat(session: HabboSession, message: String) {
        if (!arena.users.containsKey(session.userInformation.id)) return
        val chatPayload = Game2GameChatFromPlayerData(session.userInformation.id, message)
        sendHabboResponse(Outgoing.GAME_2_GAME_CHAT_FROM_PLAYER, chatPayload)
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
