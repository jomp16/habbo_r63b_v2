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

package ovh.rwx.habbo.config

import com.fasterxml.jackson.annotation.JsonProperty

data class GameConfig(
    val banzai: BanzaiConfig,
    val snowwar: SnowWarConfig = SnowWarConfig()
)

data class BanzaiConfig(
    val strict: Boolean
)

data class SnowWarConfig(
    val enabled: Boolean = true,
    @param:JsonProperty("min_players")
    val minPlayers: Int = 10,
    @param:JsonProperty("max_players")
    val maxPlayers: Int = 10,
    @param:JsonProperty("fill_with_bots")
    val fillWithBots: Boolean = true,
    @param:JsonProperty("countdown_seconds")
    val countdownSeconds: Int = 10,
    @param:JsonProperty("stage_starting_seconds")
    val stageStartingSeconds: Int = 5,
    @param:JsonProperty("game_duration_seconds")
    val gameDurationSeconds: Int = 120,
    @param:JsonProperty("free_games_daily_regular")
    val freeGamesDailyRegular: Int = 3,
    @param:JsonProperty("free_games_daily_hc")
    val freeGamesDailyHc: Int = 30,
    @param:JsonProperty("forced_arena_id")
    val forcedArenaId: Int = 0
)
