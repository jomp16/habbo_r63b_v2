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
import ovh.rwx.habbo.database.snowwar.SnowWarDao
import ovh.rwx.habbo.game.snowwar.enums.SnowWarFieldType
import ovh.rwx.habbo.game.snowwar.enums.SnowWarGameType
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Gerenciador central de SnowWar (Game2).
 * Coordena lobbies, matchmaking, estatísticas dos jogadores e delega consultas de ranking
 * para [leaderboardService].
 */
class SnowWarManager {
    private val log = LoggerFactory.getLogger(SnowWarManager::class.java)

    val lobbies = ConcurrentHashMap<Int, SnowWarLobby>()
    private val playerStatsCache = ConcurrentHashMap<Int, SnowWarPlayerStats>()
    private val lobbyIdCounter = AtomicInteger(1)

    val leaderboardService = SnowWarLeaderboardService(this)

    @Volatile
    var forcedArenaId: Int? = null

    fun getEffectiveFieldType(): SnowWarFieldType {
        val forced = forcedArenaId ?: HabboServer.habboConfig.gameConfig.snowwar.forcedArenaId.takeIf { it > 0 }
        return if (forced != null && forced > 0) {
            SnowWarFieldType.fromId(forced)
        } else {
            SnowWarFieldType.random()
        }
    }

    fun load() {
        SnowWarArenaMaps.loadArenas()
    }

    fun getPlayerStats(userId: Int): SnowWarPlayerStats {
        return playerStatsCache.computeIfAbsent(userId) {
            val stats = SnowWarDao.getStats(userId) ?: SnowWarPlayerStats(userId)
            val (year, week) = getCurrentYearAndWeek()
            if (stats.lastYear != year || stats.lastWeekNumber != week) {
                stats.weeklyScore = 0
                stats.weeklyGamesPlayed = 0
                stats.lastYear = year
                stats.lastWeekNumber = week
            }
            stats
        }
    }

    fun savePlayerStats(stats: SnowWarPlayerStats) {
        playerStatsCache[stats.userId] = stats
        SnowWarDao.saveStats(stats)
    }

    fun getFreeGamesLeft(session: HabboSession): Int {
        val cfg = HabboServer.habboConfig.gameConfig.snowwar
        val maxFree = if (session.habboSubscription.hasHabboClub) cfg.freeGamesDailyHc else cfg.freeGamesDailyRegular
        val stats = getPlayerStats(session.userInformation.id)
        return (maxFree - stats.weeklyGamesPlayed).coerceAtLeast(0)
    }

    fun quickJoin(session: HabboSession) {
        ejectPlayerFromLobbies(session)
        var lobby = lobbies.values.firstOrNull { !it.started && it.users.size < it.maximumPlayers }
        if (lobby == null) {
            val id = lobbyIdCounter.getAndIncrement()
            val fieldType = getEffectiveFieldType()
            lobby = SnowWarLobby(
                gameId = id,
                levelName = "snowwar_stage_1",
                gameType = SnowWarGameType.SNOW_WAR.id,
                fieldType = fieldType,
                numberOfTeams = 2,
                maximumPlayers = HabboServer.habboConfig.gameConfig.snowwar.maxPlayers,
                owningPlayerName = session.userInformation.username
            )
            lobbies[id] = lobby
            log.info(
                "[SnowWar] Created new lobby id={} fieldType={} (arenaId={}) by {}",
                id,
                fieldType,
                fieldType.id,
                session.userInformation.username
            )
        } else {
            log.info(
                "[SnowWar] Player {} joining existing lobby id={} fieldType={}",
                session.userInformation.username,
                lobby.gameId,
                lobby.fieldType
            )
        }
        lobby.addPlayer(session)
    }

    private fun ejectPlayerFromLobbies(session: HabboSession) {
        lobbies.values.forEach { lobby ->
            lobby.removePlayer(session)
        }
    }

    fun startSnowWar(session: HabboSession, levelName: String) {
        ejectPlayerFromLobbies(session)
        val id = lobbyIdCounter.getAndIncrement()
        val fieldType = getEffectiveFieldType()
        val lobby = SnowWarLobby(
            gameId = id,
            levelName = levelName.ifEmpty { "snowwar_stage_1" },
            gameType = SnowWarGameType.SNOW_WAR.id,
            fieldType = fieldType,
            numberOfTeams = 2,
            maximumPlayers = HabboServer.habboConfig.gameConfig.snowwar.maxPlayers,
            owningPlayerName = session.userInformation.username
        )
        lobbies[id] = lobby
        log.info(
            "[SnowWar] Started custom game lobby id={} fieldType={} (arenaId={}) by {}",
            id,
            fieldType,
            fieldType.id,
            session.userInformation.username
        )
        lobby.addPlayer(session)
    }

    fun leaveGame(session: HabboSession, exit: Boolean) {
        val lobby = getLobbyForPlayer(session) ?: return
        log.info(
            "[SnowWar] Player {} leaving lobby id={} (exit={})",
            session.userInformation.username,
            lobby.gameId,
            exit
        )
        if (exit) lobby.removePlayer(session)
    }

    fun getLobbyForPlayer(session: HabboSession): SnowWarLobby? =
        lobbies.values.lastOrNull { it.users.containsKey(session.userInformation.id) }

    fun getGameForPlayer(session: HabboSession): SnowWarGame? =
        getLobbyForPlayer(session)?.activeGame

    fun removeLobby(gameId: Int) {
        val removed = lobbies.remove(gameId)
        if (removed != null) {
            log.info("[SnowWar] Removed lobby id={} fieldType={}", gameId, removed.fieldType)
        }
    }

    // ------------------------------------------------------------------
    // Delegações de Leaderboard
    // ------------------------------------------------------------------

    fun getCurrentYearAndWeek(): Pair<Int, Int> =
        leaderboardService.getCurrentYearAndWeek()

    fun getMinutesUntilReset(): Int =
        leaderboardService.getMinutesUntilReset()

    fun getFriendsLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> =
        leaderboardService.getFriendsLeaderboard(session, gameTypeId, rank, direction, pageSize, maxEntries)

    fun getWeeklyFriendsLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        offset: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> =
        leaderboardService.getWeeklyFriendsLeaderboard(session, gameTypeId, offset, rank, direction, pageSize, maxEntries)

    fun getTotalLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> =
        leaderboardService.getTotalLeaderboard(session, gameTypeId, rank, direction, pageSize, maxEntries)

    fun getWeeklyLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        offset: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> =
        leaderboardService.getWeeklyLeaderboard(session, gameTypeId, offset, rank, direction, pageSize, maxEntries)

    fun getTotalGroupLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> =
        leaderboardService.getTotalGroupLeaderboard(session, gameTypeId, rank, direction, pageSize, maxEntries)

    fun getWeeklyGroupLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        offset: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> =
        leaderboardService.getWeeklyGroupLeaderboard(session, gameTypeId, offset, rank, direction, pageSize, maxEntries)
}
