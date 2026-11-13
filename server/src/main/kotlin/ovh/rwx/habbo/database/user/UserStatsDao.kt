/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.database.user

import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.database.writebehind.WriteKey
import ovh.rwx.habbo.game.user.information.UserStats
import java.time.LocalDateTime
import ovh.rwx.habbo.database.db

object UserStatsDao {
    private const val LAST_ONLINE_TTL_MS = 120_000L
    private val lastOnlineCache = java.util.concurrent.ConcurrentHashMap<Int, Pair<LocalDateTime, Long>>()

    /**
     * Último acesso de um usuário (exibido para amigos offline no messenger). Usa o cache pré-carregado por
     * [prefetchLastOnline] (TTL curto) e só cai no banco quando não há entrada válida.
     */
    fun getLastOnline(userId: Int): LocalDateTime {
        val cached = lastOnlineCache[userId]
        if (cached != null && System.currentTimeMillis() - cached.second < LAST_ONLINE_TTL_MS) return cached.first
        return getUserStats(userId).lastOnline
    }

    /** Carrega em UMA query o último acesso de vários usuários, evitando uma ida ao banco por amigo. */
    fun prefetchLastOnline(userIds: Collection<Int>) {
        val ids = userIds.filter { it != UserInformationDao.serverConsoleUserInformation.id }.distinct()
        if (ids.isEmpty()) return

        val now = System.currentTimeMillis()
        db {
            query<UserLastOnlineDto>(
                "SELECT `user_id`, `last_online` FROM `users_stats` WHERE `user_id` IN (${ids.joinToString(",")})"
            )
        }.forEach { lastOnlineCache[it.userId] = it.lastOnline to now }
    }

    private val serverConsoleUserStats: UserStats = UserStats(UserInformationDao.serverConsoleUserInformation.id,
            LocalDateTime.now(),
            LocalDateTime.now(),
            Int.MAX_VALUE.toLong(),
            Int.MAX_VALUE,
            Int.MAX_VALUE,
            Int.MAX_VALUE,
            Int.MAX_VALUE,
            Int.MAX_VALUE,
            Int.MAX_VALUE,
            Int.MAX_VALUE,
            Int.MAX_VALUE,
            0,
            0,
            0,
            Int.MAX_VALUE,
            Int.MAX_VALUE,
            LocalDateTime.now(),
            LocalDateTime.now())

    fun getUserStats(userId: Int): UserStats {
        if (userId == UserInformationDao.serverConsoleUserInformation.id) return serverConsoleUserStats
        val userStats = db {
            queryOne<UserStatsDto>(
                "sql/users/stats/select_user_stats.sql",
                mapOf("user_id" to userId)
            )?.toDomain()
        }

        if (userStats == null) {
            // no users stats, create it
            db {
                insertAndGetGeneratedKey(javaClass.classLoader.getResource("sql/users/stats/insert_user_stats.sql").readText(),
                        mapOf(
                                "id" to userId
                        )
                )
            }
            // Now fetch it again, doing a one recursive call, and returns this
            return getUserStats(userId)
        }

        if (userStats.respectLastUpdate.plusDays(1).isBefore(LocalDateTime.now()) && (userStats.dailyRespectPoints < 3 || userStats.dailyPetRespectPoints < 3 || userStats.dailyCompetitionVotes < 3)) {
            userStats.respectLastUpdate = LocalDateTime.now()

            userStats.dailyRespectPoints = 3
            userStats.dailyPetRespectPoints = 3
            userStats.dailyCompetitionVotes = 3
        }

        return userStats
    }

    /**
     * Presente recebido por quem está OFFLINE: soma 1 direto na linha (write-behind). No próximo login o contador
     * carregado alimenta o ACH_GiftReceiver (ver processLoginAchievements).
     */
    fun addGiftReceived(userId: Int) = WriteBehindManager.queue(WriteKey.user(userId)) {
        db {
            update(
                "sql/users/stats/increment_gifts_received.sql",
                mapOf("user_id" to userId)
            )
        }
    }

    fun saveStats(userStats: UserStats) {
        db {
            update(javaClass.classLoader.getResource("sql/users/stats/update_user_stats.sql").readText(),
                    mapOf(
                            "last_online" to userStats.lastOnlineDatabase,
                            "credits_last_update" to userStats.creditsLastUpdate,
                            "favorite_group" to if (userStats.favoriteGroupId == 0) null else userStats.favoriteGroupId,
                            "online_seconds" to userStats.totalOnlineSeconds,
                            "respect" to userStats.respect,
                            "daily_respect_points" to userStats.dailyRespectPoints,
                            "daily_pet_respect_points" to userStats.dailyPetRespectPoints,
                            "respect_last_update" to userStats.respectLastUpdate,
                            "marketplace_tickets" to userStats.marketplaceTickets,
                            "room_visits" to userStats.roomVisits,
                            "gifts_given" to userStats.giftsGiven,
                            "gifts_received" to userStats.giftsReceived,
                            "id" to userStats.id
                    )
            )
        }
    }
}

data class UserStatsDto(
    val id: Int,
    val lastOnline: LocalDateTime = LocalDateTime.now(),
    val onlineSeconds: Long = 0,
    val roomVisits: Int = 0,
    val respect: Int = 0,
    val giftsGiven: Int = 0,
    val giftsReceived: Int = 0,
    val dailyRespectPoints: Int = 3,
    val dailyPetRespectPoints: Int = 3,
    val dailyCompetitionVotes: Int = 3,
    val achievementScore: Int = 0,
    val questId: Int = 0,
    val questProgress: Int = 0,
    val favoriteGroup: Int? = 0,
    val ticketsAnswered: Int = 0,
    val marketplaceTickets: Int = 0,
    val creditsLastUpdate: LocalDateTime = LocalDateTime.now(),
    val respectLastUpdate: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): UserStats = UserStats(
        id, LocalDateTime.now(), lastOnline, onlineSeconds, roomVisits, respect,
        giftsGiven, giftsReceived, dailyRespectPoints, dailyPetRespectPoints,
        dailyCompetitionVotes, achievementScore, questId, questProgress,
        favoriteGroup ?: 0, ticketsAnswered, marketplaceTickets,
        creditsLastUpdate, respectLastUpdate
    )
}

data class UserLastOnlineDto(
    val userId: Int,
    val lastOnline: LocalDateTime
)
