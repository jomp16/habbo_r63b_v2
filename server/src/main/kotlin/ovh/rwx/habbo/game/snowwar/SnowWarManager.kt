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
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.snowwar.enums.SnowWarFieldType
import ovh.rwx.habbo.game.snowwar.enums.SnowWarGameType
import ovh.rwx.habbo.game.user.HabboSession
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SnowWarManager {
    private val log = LoggerFactory.getLogger(SnowWarManager::class.java)
    val lobbies = ConcurrentHashMap<Int, SnowWarLobby>()
    private val playerStatsCache = ConcurrentHashMap<Int, SnowWarPlayerStats>()
    private val lobbyIdCounter = AtomicInteger(1)

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

    fun getLobbyForPlayer(session: HabboSession): SnowWarLobby? {
        return lobbies.values.lastOrNull { it.users.containsKey(session.userInformation.id) }
    }

    fun removeLobby(gameId: Int) {
        val removed = lobbies.remove(gameId)
        if (removed != null) {
            log.info("[SnowWar] Removed lobby id={} fieldType={}", gameId, removed.fieldType)
        }
    }

    fun getCurrentYearAndWeek(): Pair<Int, Int> {
        val now = LocalDate.now()
        val weekFields = WeekFields.ISO
        val year = now.get(weekFields.weekBasedYear())
        val week = now.get(weekFields.weekOfWeekBasedYear())
        return Pair(year, week)
    }

    fun getMinutesUntilReset(): Int {
        val now = LocalDateTime.now()
        val nextMonday = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .withHour(0).withMinute(0).withSecond(0).withNano(0)
        return Duration.between(now, nextMonday).toMinutes().toInt().coerceAtLeast(0)
    }

    fun getFriendsLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> {
        val friendIds = mutableSetOf<Int>()
        friendIds.addAll(session.habboMessenger.friends.keys)
        friendIds.add(session.userInformation.id)

        val rawEntries = friendIds.mapNotNull { uid: Int ->
            val stats = getPlayerStats(uid)
            val userInfo = if (uid == session.userInformation.id) {
                session.userInformation
            } else {
                UserInformationDao.getUserInformationById(uid)
            }
            if (userInfo == null) null
            else {
                SnowWarLeaderboardUserEntry(
                    userId = uid,
                    score = stats.totalScore,
                    rank = 0,
                    name = userInfo.username,
                    figure = userInfo.figure,
                    gender = userInfo.gender
                )
            }
        }.sortedWith(compareByDescending<SnowWarLeaderboardUserEntry> { it.score }.thenBy { it.userId })

        val entries = rawEntries.mapIndexed { index, entry ->
            SnowWarLeaderboardUserEntry(
                userId = entry.userId,
                score = entry.score,
                rank = index + 1,
                name = entry.name,
                figure = entry.figure,
                gender = entry.gender
            )
        }

        val totalListSize = entries.size
        val limitedEntries = if (maxEntries > 0) entries.take(maxEntries) else entries
        return Pair(limitedEntries, totalListSize)
    }

    fun getWeeklyFriendsLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        offset: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> {
        val friendIds = mutableSetOf<Int>()
        friendIds.addAll(session.habboMessenger.friends.keys)
        friendIds.add(session.userInformation.id)

        val rawEntries = friendIds.mapNotNull { uid: Int ->
            val stats = getPlayerStats(uid)
            val userInfo = if (uid == session.userInformation.id) {
                session.userInformation
            } else {
                UserInformationDao.getUserInformationById(uid)
            }
            if (userInfo == null) null
            else {
                SnowWarLeaderboardUserEntry(
                    userId = uid,
                    score = stats.weeklyScore,
                    rank = 0,
                    name = userInfo.username,
                    figure = userInfo.figure,
                    gender = userInfo.gender
                )
            }
        }.sortedWith(compareByDescending<SnowWarLeaderboardUserEntry> { it.score }.thenBy { it.userId })

        val entries = rawEntries.mapIndexed { index, entry ->
            SnowWarLeaderboardUserEntry(
                userId = entry.userId,
                score = entry.score,
                rank = index + 1,
                name = entry.name,
                figure = entry.figure,
                gender = entry.gender
            )
        }

        val totalListSize = entries.size
        val limitedEntries = if (maxEntries > 0) entries.take(maxEntries) else entries
        return Pair(limitedEntries, totalListSize)
    }

    fun getTotalLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> {
        val limit = if (maxEntries > 0) maxEntries else 50
        val dbEntries = SnowWarDao.getTotalLeaderboard(limit, 0)
        val result = dbEntries.toMutableList()

        val myStats = getPlayerStats(session.userInformation.id)
        val myRank = (dbEntries.indexOfFirst { it.userId == session.userInformation.id }.takeIf { it >= 0 }?.plus(1))
            ?: (dbEntries.size + 1)

        result.add(
            SnowWarLeaderboardUserEntry(
                userId = session.userInformation.id,
                score = myStats.totalScore,
                rank = myRank,
                name = session.userInformation.username,
                figure = session.userInformation.figure,
                gender = session.userInformation.gender
            )
        )

        return Pair(result, dbEntries.size)
    }

    fun getWeeklyLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        offset: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> {
        val (year, week) = getCurrentYearAndWeek()
        val limit = if (maxEntries > 0) maxEntries else 50
        val targetWeek = (week - offset).coerceAtLeast(1)
        val dbEntries = SnowWarDao.getWeeklyLeaderboard(year, targetWeek, limit, 0)
        val result = dbEntries.toMutableList()

        val myStats = getPlayerStats(session.userInformation.id)
        val myRank = (dbEntries.indexOfFirst { it.userId == session.userInformation.id }.takeIf { it >= 0 }?.plus(1))
            ?: (dbEntries.size + 1)

        result.add(
            SnowWarLeaderboardUserEntry(
                userId = session.userInformation.id,
                score = myStats.weeklyScore,
                rank = myRank,
                name = session.userInformation.username,
                figure = session.userInformation.figure,
                gender = session.userInformation.gender
            )
        )

        return Pair(result, dbEntries.size)
    }

    fun getTotalGroupLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> {
        return Pair(emptyList(), 0)
    }

    fun getWeeklyGroupLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        offset: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> {
        return Pair(emptyList(), 0)
    }
}
