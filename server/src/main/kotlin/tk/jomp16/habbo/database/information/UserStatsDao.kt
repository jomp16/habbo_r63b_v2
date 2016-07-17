/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.database.information

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.user.information.UserStats
import tk.jomp16.habbo.kotlin.insertAndGetGeneratedKey
import tk.jomp16.habbo.kotlin.localDateTime

object UserStatsDao {
    fun getUserStats(userId: Int): UserStats {
        val tmp = HabboServer.database {
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

        if (tmp == null) {
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

        return tmp
    }
}