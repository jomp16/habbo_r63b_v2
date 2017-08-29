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

package tk.jomp16.habbo.config

import com.fasterxml.jackson.annotation.JsonProperty

data class RoomTaskConfig(
        @JsonProperty("threads", required = true)
        val threads: Int,
        @JsonProperty("max_room_per_thread", required = true)
        val maxRoomPerThread: Int,
        @JsonProperty("delay_milliseconds", required = true)
        val delayMilliseconds: Int,
        @JsonProperty("error_threshold", required = true)
        val errorThreshold: Int,
        @JsonProperty("save_item_seconds", required = true)
        val saveItemSeconds: Int,
        @JsonProperty("empty_room_seconds", required = true)
        val emptyRoomSeconds: Int)