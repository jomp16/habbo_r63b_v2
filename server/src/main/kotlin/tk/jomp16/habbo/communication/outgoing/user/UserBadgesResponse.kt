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

package tk.jomp16.habbo.communication.outgoing.user

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.badge.Badge

@Suppress("unused", "UNUSED_PARAMETER")
class UserBadgesResponse {
    @Response(Outgoing.USER_BADGES)
    fun response(habboResponse: HabboResponse, id: Int, badges: Collection<Badge>) {
        habboResponse.apply {
            val equippedBadges = badges.filter { it.slot > 0 }

            // todo: make sure if it's always user id or use virtualID when if in room
            writeInt(id)
            writeInt(equippedBadges.size)

            equippedBadges.forEach {
                writeInt(it.slot)
                writeUTF(it.code)
            }
        }
    }
}