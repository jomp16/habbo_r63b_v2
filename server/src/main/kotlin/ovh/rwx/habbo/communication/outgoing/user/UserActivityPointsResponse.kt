/*
 * Copyright (C) 2015-2020 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.user

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class UserActivityPointsResponse {
    @Response(Outgoing.ACTIVITY_POINTS_BALANCE)
    fun response(habboResponse: HabboResponse, pixels: Int, vipPoints: Int) {
        habboResponse.apply {
            writeInt(2)
            writeInt(ActivityPointType.PIXELS.code) // pixels / duckets
            writeInt(pixels)
            writeInt(ActivityPointType.DIAMONDS.code) // diamonds
            writeInt(vipPoints)
        }
    }
}

enum class ActivityPointType(val code: Int) {
    PIXELS(0),
    SEASHELLS(1),
    HEARTS(2),
    GIFT_POINTS(3),
    SHELLS(4),
    DIAMONDS(5),
    SEASHELLS_2(101),
    NUTS(102),
    STARS(103),
    CLOUDS(104),
    DIAMONDS_2(105),
}