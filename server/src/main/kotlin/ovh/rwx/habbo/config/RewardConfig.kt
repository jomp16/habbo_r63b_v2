/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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

data class RewardConfig(
        @JsonProperty("credits", required = true)
        val credits: Int,
        @JsonProperty("credits_max", required = true)
        val creditsMax: Int,
        @JsonProperty("pixels", required = true)
        val pixels: Int,
        @JsonProperty("pixels_max", required = true)
        val pixelsMax: Int,
        @JsonProperty("vip_points", required = true)
        val vipPoints: Int,
        @JsonProperty("vip_points_max", required = true)
        val vipPointsMax: Int
)