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

package ovh.rwx.habbo.game.badge

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.badge.BadgeDao
import ovh.rwx.habbo.game.user.badge.Badge
import java.util.concurrent.ConcurrentHashMap

class BadgeManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val badgeOwnerCounts: MutableMap<String, Int> = ConcurrentHashMap()
    val userBadges: MutableMap<Int, Map<String, Badge>> = ConcurrentHashMap()

    fun load() {
        badgeOwnerCounts.clear()
        userBadges.clear()

        badgeOwnerCounts.putAll(BadgeDao.getOwnerCounts())
        userBadges.putAll(BadgeDao.getAllBadges())

        log.info("Loaded {} badge owner counts!", badgeOwnerCounts.size)
        log.info("Loaded badges for {} users!", userBadges.size)
    }

    fun getOwnerCount(code: String): Int = badgeOwnerCounts[code] ?: 0

    fun getBadges(userId: Int): Map<String, Badge> = userBadges[userId] ?: emptyMap()

    fun getBadgeCount(userId: Int): Int = getBadges(userId).size

    fun addBadge(userId: Int, badge: Badge) {
        var added = false
        userBadges.compute(userId) { _, current ->
            val map = current ?: emptyMap()
            if (!map.containsKey(badge.code)) {
                added = true
                map + (badge.code to badge)
            } else {
                map
            }
        }
        if (added) {
            badgeOwnerCounts.compute(badge.code) { _, count -> (count ?: 0) + 1 }
        }
    }

    fun removeBadge(userId: Int, code: String) {
        var removed = false
        userBadges.computeIfPresent(userId) { _, current ->
            if (current.containsKey(code)) {
                removed = true
                val updated = current - code
                if (updated.isEmpty()) null else updated
            } else {
                current
            }
        }

        if (removed) {
            badgeOwnerCounts.computeIfPresent(code) { _, count ->
                if (count <= 1) null else count - 1
            }
        }
    }
}
