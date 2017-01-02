/*
 * Copyright (C) 2017 jomp16
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

package tk.jomp16.habbo.database.information

import net.sf.ehcache.Ehcache
import net.sf.ehcache.Element
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.user.information.UserStats
import tk.jomp16.habbo.kotlin.addAndGetEhCache
import tk.jomp16.habbo.kotlin.insertAndGetGeneratedKey
import tk.jomp16.habbo.kotlin.localDateTime
import java.time.Clock
import java.time.LocalDateTime

object UserStatsDao {
    private val userStatsCache: Ehcache = HabboServer.cacheManager.addAndGetEhCache("userStatsCache")

    fun getUserStats(userId: Int, cache: Boolean = true): UserStats {
        if (!cache || !userStatsCache.isKeyInCache(userId)) {
            val userStats = HabboServer.database {
                select("SELECT * FROM users_stats WHERE user_id = :user_id LIMIT 1",
                        mapOf(
                                "user_id" to userId
                        )
                ) {
                    UserStats(
                            it.int("id"),
                            it.localDateTime("last_online"),
                            it.long("online_seconds"),
                            it.int("room_visits"),
                            it.int("respect"),
                            it.int("gifts_given"),
                            it.int("gifts_received"),
                            it.int("daily_respect_points"),
                            it.int("daily_pet_respect_points"),
                            it.int("daily_competition_votes"),
                            it.int("achievement_score"),
                            it.int("quest_id"),
                            it.int("quest_progress"),
                            it.int("favorite_group"),
                            it.int("tickets_answered"),
                            it.int("marketplace_tickets"),
                            it.localDateTime("credits_last_update"),
                            it.localDateTime("respect_last_update")
                    )
                }.firstOrNull()
            }

            if (userStats == null) {
                // no users stats, create it
                HabboServer.database {
                    insertAndGetGeneratedKey("INSERT INTO users_stats (user_id) VALUES (:id)",
                            mapOf(
                                    "id" to userId
                            )
                    )
                }

                // Now fetch it again, doing a one recursive call, and returns this
                return getUserStats(userId)
            }

            userStatsCache.put(Element(userId, userStats))
        }

        return userStatsCache.get(userId).objectValue as UserStats
    }

    fun saveStats(userStats: UserStats, lastOnline: LocalDateTime = LocalDateTime.now(Clock.systemUTC())) {
        HabboServer.database {
            update("UPDATE users_stats SET last_online = :last_online, credits_last_update = :credits_last_update, " +
                    "favorite_group = :favorite_group, online_seconds = :online_seconds, respect = :respect, " +
                    "daily_respect_points = :daily_respect_points, daily_pet_respect_points = :daily_pet_respect_points, " +
                    "respect_last_update = :respect_last_update, marketplace_tickets = :marketplace_tickets WHERE id = :id",
                    mapOf(
                            "last_online" to lastOnline,
                            "credits_last_update" to userStats.creditsLastUpdate,
                            "favorite_group" to userStats.favoriteGroup,
                            "online_seconds" to userStats.totalOnlineSeconds,
                            "respect" to userStats.respect,
                            "daily_respect_points" to userStats.dailyRespectPoints,
                            "daily_pet_respect_points" to userStats.dailyPetRespectPoints,
                            "respect_last_update" to userStats.respectLastUpdate,
                            "marketplace_tickets" to userStats.marketplaceTickets,
                            "id" to userStats.id
                    )
            )
        }
    }
}