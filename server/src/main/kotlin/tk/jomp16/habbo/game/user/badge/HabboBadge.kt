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

package tk.jomp16.habbo.game.user.badge

import tk.jomp16.habbo.communication.QueuedHabboResponse
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.badge.BadgeDao
import tk.jomp16.habbo.game.user.HabboSession
import java.util.*

class HabboBadge(private val habboSession: HabboSession) {
    val badges: MutableMap<String, Badge> = HashMap(BadgeDao.getBadges(habboSession.userInformation.id).associateBy { it.code })

    fun resetSlots() {
        badges.values.forEach { it.slot = 0 }
    }

    fun saveAll() {
        BadgeDao.saveBadges(badges.values)
    }

    fun addBadge(code: String) {
        addBadge(code, 0)
    }

    fun addBadge(code: String, slot: Int) {
        if (badges.containsKey(code)) return

        val badge = BadgeDao.addBadge(habboSession.userInformation.id, code, slot)

        badges += badge.code to badge

        val queuedHabboResponse = QueuedHabboResponse()

        queuedHabboResponse += Outgoing.INVENTORY_BADGES to arrayOf(badges.values)
        queuedHabboResponse += Outgoing.INVENTORY_NEW_OBJECTS to arrayOf(4, badge.id)

        habboSession.sendQueuedHabboResponse(queuedHabboResponse)
    }

    fun removeBadge(badgeCode: String) {
        val badge = badges.remove(badgeCode) ?: return

        BadgeDao.removeBadge(badge.id)
    }
}