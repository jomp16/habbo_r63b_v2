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

package ovh.rwx.habbo.game.snowwar

data class SnowWarTotalLeaderboardData(
    val entries: List<SnowWarLeaderboardUserEntry> = emptyList(),
    val totalListSize: Int = 0,
    val gameTypeId: Int = 0
)

data class SnowWarTotalGroupLeaderboardData(
    val entries: List<SnowWarLeaderboardUserEntry> = emptyList(),
    val totalListSize: Int = 0,
    val gameTypeId: Int = 0,
    val favouriteGroupId: Int = 0
)

data class SnowWarWeeklyLeaderboardData(
    val year: Int = 0,
    val week: Int = 0,
    val maxOffset: Int = 4,
    val currentOffset: Int = 0,
    val minutesUntilReset: Int = 0,
    val entries: List<SnowWarLeaderboardUserEntry> = emptyList(),
    val totalListSize: Int = 0,
    val gameTypeId: Int = 0
)

data class SnowWarWeeklyGroupLeaderboardData(
    val year: Int = 0,
    val week: Int = 0,
    val maxOffset: Int = 4,
    val currentOffset: Int = 0,
    val minutesUntilReset: Int = 0,
    val entries: List<SnowWarLeaderboardUserEntry> = emptyList(),
    val totalListSize: Int = 0,
    val gameTypeId: Int = 0,
    val favouriteGroupId: Int = 0
)
