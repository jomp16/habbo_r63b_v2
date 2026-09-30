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

package ovh.rwx.habbo.game.user.information

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.writebehind.AbstractDirtyEntity
import ovh.rwx.habbo.game.group.Group
import java.time.Duration
import java.time.LocalDateTime

class UserStats(
    val id: Int,
    var lastOnline: LocalDateTime,
    var lastOnlineDatabase: LocalDateTime,
    private var onlineSeconds: Long,
    roomVisits: Int,
    respect: Int,
    giftsGiven: Int,
    giftsReceived: Int,
    dailyRespectPoints: Int,
    dailyPetRespectPoints: Int,
    dailyCompetitionVotes: Int,
    achievementScore: Int,
    questId: Int,
    questProgress: Int,
    favoriteGroupId: Int,
    ticketsAnswered: Int,
    marketplaceTickets: Int,
    creditsLastUpdate: LocalDateTime,
    respectLastUpdate: LocalDateTime
) : AbstractDirtyEntity() {
    var roomVisits: Int = roomVisits; set(v) { if (field != v) { field = v; markDirty() } }
    var respect: Int = respect; set(v) { if (field != v) { field = v; markDirty() } }
    var giftsGiven: Int = giftsGiven; set(v) { if (field != v) { field = v; markDirty() } }
    var giftsReceived: Int = giftsReceived; set(v) { if (field != v) { field = v; markDirty() } }
    var dailyRespectPoints: Int = dailyRespectPoints; set(v) { if (field != v) { field = v; markDirty() } }
    var dailyPetRespectPoints: Int = dailyPetRespectPoints; set(v) { if (field != v) { field = v; markDirty() } }
    var dailyCompetitionVotes: Int = dailyCompetitionVotes; set(v) { if (field != v) { field = v; markDirty() } }
    var achievementScore: Int = achievementScore; set(v) { if (field != v) { field = v; markDirty() } }
    var questId: Int = questId; set(v) { if (field != v) { field = v; markDirty() } }
    var questProgress: Int = questProgress; set(v) { if (field != v) { field = v; markDirty() } }
    var favoriteGroupId: Int = favoriteGroupId; set(v) { if (field != v) { field = v; markDirty() } }
    var ticketsAnswered: Int = ticketsAnswered; set(v) { if (field != v) { field = v; markDirty() } }
    var marketplaceTickets: Int = marketplaceTickets; set(v) { if (field != v) { field = v; markDirty() } }
    var creditsLastUpdate: LocalDateTime = creditsLastUpdate; set(v) { if (field != v) { field = v; markDirty() } }
    var respectLastUpdate: LocalDateTime = respectLastUpdate; set(v) { if (field != v) { field = v; markDirty() } }

    val totalOnlineSeconds: Long
        get() = Duration.between(lastOnline, LocalDateTime.now()).seconds + onlineSeconds
    val favoriteGroup: Group?
        get() = if (favoriteGroupId == 0) null else HabboServer.habboGame.groupManager.groups[favoriteGroupId]
    var firstLoginOfDay: Boolean = false

    override fun flush() {
        ovh.rwx.habbo.database.user.UserStatsDao.saveStats(this)
        markClean()
    }
}