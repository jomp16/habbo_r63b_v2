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

package tk.jomp16.habbo.communication.incoming.room

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.util.Rotation

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUserLookToHandler {
    @Handler(Incoming.ROOM_LOOK_TO)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null) return

        val x = habboRequest.readInt()
        val y = habboRequest.readInt()

        if (habboSession.roomUser!!.currentVector3.x == x && habboSession.roomUser!!.currentVector3.y == y
                || habboSession.roomUser!!.statusMap.containsKey("sit")
                || habboSession.roomUser!!.statusMap.containsKey("lay")) {
            return
        }

        val rotation = Rotation.calculate(habboSession.roomUser!!.currentVector3.x, habboSession.roomUser!!.currentVector3.y, x, y)

        var update = false

        if (habboSession.roomUser?.bodyRotation != rotation) {
            habboSession.roomUser?.bodyRotation = rotation
            update = true
        }

        if (habboSession.roomUser?.headRotation != rotation) {
            habboSession.roomUser?.headRotation = rotation
            update = true
        }

        habboSession.roomUser?.updateNeeded = update
    }
}