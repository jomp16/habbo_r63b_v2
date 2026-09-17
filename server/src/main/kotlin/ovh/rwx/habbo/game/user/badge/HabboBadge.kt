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

package ovh.rwx.habbo.game.user.badge

import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.inventory.InventoryBadgesData
import ovh.rwx.habbo.communication.outgoing.inventory.UnseenItemCategory
import ovh.rwx.habbo.communication.outgoing.inventory.UnseenItemsData
import ovh.rwx.habbo.database.badge.BadgeDao
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.ConcurrentHashMap

class HabboBadge(private val habboSession: HabboSession) {
    val badges: MutableMap<String, Badge> = ConcurrentHashMap()
    private var initialized: Boolean = false

    fun load() {
        if (!initialized) {
            badges.putAll(BadgeDao.getBadges(habboSession.userInformation.id))

            initialized = true
        }
    }

    fun resetSlots() {
        badges.values.forEach { it.slot = 0 }
    }

    fun addBadge(code: String) {
        if (badges.containsKey(code)) return
        val badge = BadgeDao.addBadge(habboSession.userInformation.id, code, 0)

        badges[badge.code] = badge

        habboSession.sendResponse(
            Outgoing.INVENTORY_BADGES,
            OutgoingR63A.INVENTORY_BADGES,
            InventoryBadgesData(badges.values)
        )
        habboSession.sendResponse(
            Outgoing.USER_BADGES,
            OutgoingR63A.USER_BADGES,
            habboSession.userInformation.id,
            badges.values
        )
        habboSession.sendResponse(
            Outgoing.INVENTORY_UNSEEN_ITEMS,
            OutgoingR63A.INVENTORY_UNSEEN_ITEMS,
            UnseenItemsData.single(
                UnseenItemCategory.BADGE, listOf(badge.id)
            )
        )
    }

    fun removeBadge(badgeCode: String) {
        val badge = badges.remove(badgeCode) ?: return

        habboSession.sendResponse(
            Outgoing.INVENTORY_BADGES,
            OutgoingR63A.INVENTORY_BADGES,
            InventoryBadgesData(badges.values)
        )
        habboSession.sendResponse(
            Outgoing.USER_BADGES,
            OutgoingR63A.USER_BADGES,
            habboSession.userInformation.id,
            badges.values
        )

        BadgeDao.removeBadge(habboSession.userInformation.id, badge.id, badge.code)
    }
}