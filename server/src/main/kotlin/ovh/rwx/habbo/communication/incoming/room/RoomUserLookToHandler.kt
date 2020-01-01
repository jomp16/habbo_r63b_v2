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

package ovh.rwx.habbo.communication.incoming.room

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.Rotation

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUserLookToHandler {
    @Handler(Incoming.ROOM_LOOK_TO)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null) return
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