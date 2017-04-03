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

import tk.jomp16.habbo.BADGES_KEY_CACHE
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.user.badge.Badge
import tk.jomp16.habbo.kotlin.insertAndGetGeneratedKey
import tk.jomp16.habbo.util.ICacheable

@Suppress("UNCHECKED_CAST")
object BadgeDao : ICacheable {
    val badgeCache: MutableMap<Int, MutableMap<String, Badge>> = HashMap()

    override fun cacheAll() {
        cacheBadges()
    }

    override fun saveCache() {
        HabboServer.saveCache(BADGES_KEY_CACHE, badgeCache)
    }

    private fun cacheBadges() {
        val cache = HabboServer.loadCache(BADGES_KEY_CACHE) as MutableMap<Int, MutableMap<String, Badge>>?

        if (cache != null) {
            badgeCache.putAll(cache)
        } else {
            val badges = HabboServer.database {
                select("SELECT * FROM users_badges") {
                    it.int("user_id") to Badge(
                            it.int("id"),
                            it.string("code"),
                            it.int("slot")
                    )
                }
            }

            badges.forEach { data ->
                val userId = data.first
                val badges1: MutableMap<String, Badge> = mutableMapOf()

                badges.filter { it.first == userId }.map { it.second.code to it.second }.toMap(badges1)

                badgeCache.put(userId, badges1)
            }
        }
    }

    fun getBadges(userId: Int): MutableMap<String, Badge> {
        if (!badgeCache.containsKey(userId)) badgeCache.put(userId, mutableMapOf())

        return badgeCache[userId]!!
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

    fun addBadge(userId: Int, code: String, slot: Int): Badge = HabboServer.database {
        val id = insertAndGetGeneratedKey(
                "INSERT INTO users_badges (user_id, code, slot) VALUES (:user_id, :code, :slot)",
                mapOf(
                        "user_id" to userId,
                        "code" to code,
                        "slot" to slot
                )
        )

        val badge = Badge(id, code, slot)

        if (!badgeCache.containsKey(userId)) badgeCache.put(userId, mutableMapOf())

        badgeCache[userId]?.put(code, badge)

        return@database badge
    }

    fun saveBadges(badges: Collection<Badge>) {
        if (badges.isNotEmpty()) {
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
}