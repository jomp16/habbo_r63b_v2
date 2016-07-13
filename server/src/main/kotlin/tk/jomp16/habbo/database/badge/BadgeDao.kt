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

package tk.jomp16.habbo.database.badge

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.user.badge.Badge
import tk.jomp16.habbo.kotlin.insertAndGetGeneratedKey

object BadgeDao {
    fun getBadges(userId: Int) = HabboServer.database {
        select("SELECT * FROM users_badges WHERE user_id = :user_id",
               mapOf(
                       "user_id" to userId
                    )
              ) {
            Badge(
                    it.int("id"),
                    it.string("code"),
                    it.int("slot")
                 )
        }
    }

    fun removeBadge(id: Int) {
        HabboServer.database {
            update("DELETE FROM users_badges WHERE id = :id",
                   mapOf(
                           "id" to id
                        )
                  )
        }
    }

    fun addBadge(userId: Int, code: String, slot: Int) = HabboServer.database {
        val id = insertAndGetGeneratedKey(
                "INSERT INTO users_badges (user_id, code, slot) VALUES (:user_id, :code, :slot)",
                mapOf(
                        "user_id" to userId,
                        "code" to code,
                        "slot" to slot
                     )
                                         )

        Badge(id, code, slot)
    }

    fun saveBadges(badges: Collection<Badge>) {
        HabboServer.database {
            batchUpdate("UPDATE users_badges SET slot = :slot WHERE id = :id",
                        badges.map {
                            mapOf(
                                    "slot" to it.slot,
                                    "id" to it.id
                                 )
                        }
                       )
        }
    }
}