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

package ovh.rwx.habbo.database.badge

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.user.badge.Badge
import ovh.rwx.habbo.database.sequence.HiLoSequence
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.database.db
import org.slf4j.LoggerFactory

object BadgeDao {
    private val log = LoggerFactory.getLogger(javaClass)

    val badgeSequence = HiLoSequence("users_badges", blockSize = 500)

    fun getBadges(userId: Int): Map<String, Badge> = db {
        query<Badge>("sql/badges/select_badges.sql", mapOf("user_id" to userId))
            .associateBy { it.code }
    }

    fun getAllBadges(): Map<Int, Map<String, Badge>> = db {
        query<UserBadgeDto>("sql/badges/select_all_badges.sql")
            .groupBy({ it.userId }, { it.toBadge() })
            .mapValues { it.value.associateBy { badge -> badge.code } }
    }

    fun getOwnerCounts(): Map<String, Int> = db {
        query<BadgeOwnerCountDto>("sql/badges/select_badge_owner_counts.sql")
            .associate { it.code to it.ownerCount }
    }

    fun removeBadge(userId: Int, id: Int, code: String) {
        db {
            update(javaClass.classLoader.getResource("sql/badges/delete_badge.sql").readText(),
                    mapOf(
                            "id" to id
                    )
            )
        }

        HabboServer.habboGame.badgeManager.removeBadge(userId, code)
    }

    fun addBadge(userId: Int, code: String, slot: Int): Badge {
        val id = badgeSequence.nextId()
        val badge = Badge(id, code, slot)
        HabboServer.habboGame.badgeManager.addBadge(userId, badge)
        WriteBehindManager.queue {
            db {
                update(
                    "INSERT INTO `users_badges` (`id`, `user_id`, `code`, `slot`) VALUES (:id, :user_id, :code, :slot)",
                    mapOf("id" to badge.id, "user_id" to userId, "code" to badge.code, "slot" to badge.slot)
                )
            }
        }
        return badge
    }

    fun saveBadges(badges: Collection<Badge>) {
        if (badges.isNotEmpty()) {
            db {
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

data class UserBadgeDto(
    val userId: Int,
    val id: Int,
    val code: String,
    val slot: Int
) {
    fun toBadge() = Badge(id, code, slot)
}

data class BadgeOwnerCountDto(
    val code: String,
    val ownerCount: Int
)

