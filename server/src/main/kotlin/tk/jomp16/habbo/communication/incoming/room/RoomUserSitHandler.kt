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

package tk.jomp16.habbo.communication.incoming.room

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUserSitHandler {
    @Handler(Incoming.ROOM_USER_SIT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null || habboSession.roomUser == null || habboSession.roomUser!!.walking || habboSession.roomUser!!.statusMap.containsKey("sit") || habboSession.roomUser!!.statusMap.containsKey("lay")) return
        val sit = habboRequest.readInt() == 1

        if (sit) {
            if (habboSession.roomUser!!.bodyRotation % 2 != 0) {
                habboSession.roomUser!!.headRotation = habboSession.roomUser!!.headRotation - 1
                habboSession.roomUser!!.bodyRotation = habboSession.roomUser!!.bodyRotation - 1
            }

            habboSession.roomUser!!.addStatus("sit", "0.55")
        } else {
            habboSession.roomUser!!.removeStatus("sit")
        }

        habboSession.roomUser!!.updateNeeded = true
    }
}