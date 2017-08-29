/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.database.badge

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.user.badge.Badge
import tk.jomp16.habbo.kotlin.insertAndGetGeneratedKey

object BadgeDao {
    fun getBadges(userId: Int): Map<String, Badge> {
        return HabboServer.database {
            select(javaClass.classLoader.getResource("sql/badges/select_badges.sql").readText(),
                    mapOf(
                            "user_id" to userId
                    )
            ) {
                Badge(
                        it.int("id"),
                        it.string("code"),
                        it.int("slot")
                )
            }.associateBy { it.code }
        }
    }

    fun removeBadge(id: Int) {
        HabboServer.database {
            update(javaClass.classLoader.getResource("sql/badges/delete_badge.sql").readText(),
                    mapOf(
                            "id" to id
                    )
            )
        }
    }

    fun addBadge(userId: Int, code: String, slot: Int): Badge = HabboServer.database {
        val id = insertAndGetGeneratedKey(javaClass.classLoader.getResource("sql/badges/insert_badge.sql").readText(),
                mapOf(
                        "user_id" to userId,
                        "code" to code,
                        "slot" to slot
                )
        )

        Badge(id, code, slot)
    }

    fun saveBadges(badges: Collection<Badge>) {
        if (badges.isNotEmpty()) {
            HabboServer.database {
                batchUpdate(javaClass.classLoader.getResource("sql/badges/update_badge.sql").readText(),
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
}