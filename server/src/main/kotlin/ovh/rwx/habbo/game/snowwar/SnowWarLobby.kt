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
import ovh.rwx.habbo.communication.outgoing.gamecenter.Game2GameChatFromPlayerData
import ovh.rwx.habbo.communication.outgoing.gamecenter.Game2UserJoinedGameData
import ovh.rwx.habbo.game.snowwar.bot.BotNameGenerator
import ovh.rwx.habbo.game.snowwar.bot.SnowWarBotData
import ovh.rwx.habbo.game.snowwar.enums.SnowWarFieldType
import ovh.rwx.habbo.game.snowwar.enums.SnowWarStageState
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTeam
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

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

    var activeGame: SnowWarGame? = null
        private set

    val state: SnowWarStageState get() = activeGame?.state ?: SnowWarStageState.INACTIVE
    var started: Boolean = false
        private set

    var countdownStarted: Boolean = false
        private set
    private var countdownFuture: ScheduledFuture<*>? = null

    fun sendHabboResponse(header: Outgoing, vararg params: Any) {
        users.values.forEach { it.session?.sendHabboResponse(header, *params) }
    }

    fun sendHabboResponseExcept(excluded: HabboSession, header: Outgoing, vararg params: Any) {
        users.values.forEach { user ->
            val session = user.session
            if (session != null && session != excluded) {
                session.sendHabboResponse(header, *params)
            }
        }
    }

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
        sendHabboResponseExcept(session, Outgoing.GAME_2_USER_JOINED_GAME, joinedPayload)

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

    fun fillWithBots() {
        if (users.size >= maximumPlayers) return
        val existingNames = users.values.map { it.name }.toMutableSet()
        var nextBotId = -1001

        while (users.size < maximumPlayers) {
            while (users.containsKey(nextBotId)) {
                nextBotId--
            }

            val team1Count = users.values.count { it.team == SnowWarTeam.BLUE }
            val team2Count = users.values.count { it.team == SnowWarTeam.RED }
            val team = if (team1Count <= team2Count) SnowWarTeam.BLUE else SnowWarTeam.RED

            val gender = if (ThreadLocalRandom.current().nextBoolean()) "M" else "F"
            val botName = BotNameGenerator.generateNickname(existingNames)
            existingNames.add(botName)

            val figure = HabboServer.habboGame.figureManager.generateRandomFigure(gender)
            val motto = BotNameGenerator.generateMotto()

            val botUser = SnowWarUser(
                session = null,
                team = team,
                botData = SnowWarBotData(
                    id = nextBotId,
                    name = botName,
                    figure = figure,
                    gender = gender,
                    mission = motto
                )
            )
            users[nextBotId] = botUser

            val joinedPayload = Game2UserJoinedGameData(botUser, false)
            sendHabboResponse(Outgoing.GAME_2_USER_JOINED_GAME, joinedPayload)

            log.info(
                "[Lobby-{}] Bot {} (id={}) joined lobby on team={}, total players: {}/{}",
                gameId,
                botName,
                nextBotId,
                team,
                users.size,
                maximumPlayers
            )

            nextBotId--
        }
    }

    fun removePlayer(session: HabboSession) {
        val userId = session.userInformation.id
        val game = activeGame
        if (game != null) {
            game.removePlayer(session)
            users.remove(userId)
            return
        }

        users.remove(userId)
        log.info(
            "[Lobby-{}] Player {} (userId={}) left lobby, remaining: {}",
            gameId,
            session.userInformation.username,
            userId,
            users.size
        )

        sendHabboResponse(Outgoing.GAME_2_USER_LEFT_GAME, userId)

        val minPlayers = HabboServer.habboConfig.gameConfig.snowwar.minPlayers
        val realPlayers = users.values.count { it.session != null }
        if (realPlayers < minPlayers && !started) {
            val bots = users.values.filter { it.session == null }.toList()
            bots.forEach { bot ->
                users.remove(bot.userId)
                sendHabboResponse(Outgoing.GAME_2_USER_LEFT_GAME, bot.userId)
            }

            if (countdownStarted) {
                countdownStarted = false
                countdownFuture?.cancel(true)
                countdownFuture = null
                log.info(
                    "[Lobby-{}] Cancelled lobby countdown: real players ({}) < minPlayers ({})",
                    gameId,
                    realPlayers,
                    minPlayers
                )
                sendHabboResponse(Outgoing.GAME_2_STOP_COUNTER)
            }
        }

        if (users.isEmpty() || realPlayers == 0) {
            countdownFuture?.cancel(true)
            HabboServer.habboGame.snowWarManager.removeLobby(gameId)
        }
    }

    fun checkCountdown() {
        val minPlayers = HabboServer.habboConfig.gameConfig.snowwar.minPlayers
        val realPlayers = users.values.count { it.session != null }
        if (!countdownStarted && !started && realPlayers >= minPlayers) {
            if (HabboServer.habboConfig.gameConfig.snowwar.fillWithBots) {
                fillWithBots()
            }
            countdownStarted = true
            val count = HabboServer.habboConfig.gameConfig.snowwar.countdownSeconds
            log.info(
                "[Lobby-{}] Starting lobby countdown: {}s (real players: {} >= minPlayers: {}, total: {}/{})",
                gameId,
                count,
                realPlayers,
                minPlayers,
                users.size,
                maximumPlayers
            )
            sendHabboResponse(Outgoing.GAME_2_START_COUNTER, count)
            countdownFuture = HabboServer.serverScheduledExecutor.schedule({
                startGame()
            }, count.toLong(), TimeUnit.SECONDS)
        }
    }

    fun startGame() {
        if (started || users.isEmpty()) return
        started = true
        countdownFuture?.cancel(true)
        countdownFuture = null

        val game = SnowWarGame(
            gameId = gameId,
            levelName = levelName,
            gameType = gameType,
            fieldType = fieldType,
            numberOfTeams = numberOfTeams,
            users = users,
            lobbyDataProvider = { toLobbyData() },
            onGameOver = {
                HabboServer.habboGame.snowWarManager.removeLobby(gameId)
            }
        )
        activeGame = game
        game.start()
    }

    fun chat(session: HabboSession, message: String) {
        if (!users.containsKey(session.userInformation.id)) return
        val chatPayload = Game2GameChatFromPlayerData(session.userInformation.id, message)
        sendHabboResponse(Outgoing.GAME_2_GAME_CHAT_FROM_PLAYER, chatPayload)
    }
}
