/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.game.group.Group
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

data class UserStats(
        val id: Int,
        var lastOnline: LocalDateTime,
        private var lastOnlineDatabase: LocalDateTime,
        private var onlineSeconds: Long,
        var roomVisits: Int,
        var respect: Int,
        var giftsGiven: Int,
        var giftsReceived: Int,
        var dailyRespectPoints: Int,
        var dailyPetRespectPoints: Int,
        var dailyCompetitionVotes: Int,
        var achievementScore: Int,
        var questId: Int,
        var questProgress: Int,
        var favoriteGroupId: Int,
        var ticketsAnswered: Int,
        var marketplaceTickets: Int,
        var creditsLastUpdate: LocalDateTime,
        var respectLastUpdate: LocalDateTime
) {
    val totalOnlineSeconds: Long
        get() = Duration.between(lastOnlineDatabase, LocalDateTime.now(Clock.systemUTC())).seconds + onlineSeconds
    val favoriteGroup: Group?
        get() = if (favoriteGroupId == 0) null else HabboServer.habboGame.groupManager.groups[favoriteGroupId]
}