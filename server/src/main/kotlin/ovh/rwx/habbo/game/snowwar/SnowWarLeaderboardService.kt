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

import ovh.rwx.habbo.database.snowwar.SnowWarDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.user.HabboSession
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

class SnowWarLeaderboardService(private val snowWarManager: SnowWarManager) {

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
            val stats = snowWarManager.getPlayerStats(uid)
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
            entry.copy(rank = index + 1)
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
            val stats = snowWarManager.getPlayerStats(uid)
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
            entry.copy(rank = index + 1)
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

        val myStats = snowWarManager.getPlayerStats(session.userInformation.id)
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

        val myStats = snowWarManager.getPlayerStats(session.userInformation.id)
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
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> = Pair(emptyList(), 0)

    fun getWeeklyGroupLeaderboard(
        session: HabboSession,
        gameTypeId: Int,
        offset: Int,
        rank: Int,
        direction: Int,
        pageSize: Int,
        maxEntries: Int
    ): Pair<List<SnowWarLeaderboardUserEntry>, Int> = Pair(emptyList(), 0)
}
