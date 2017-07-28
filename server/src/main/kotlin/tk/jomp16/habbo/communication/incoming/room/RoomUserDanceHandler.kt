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
class RoomUserDanceHandler {
    @Handler(Incoming.ROOM_USER_DANCE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null) return
        var danceId = habboRequest.readInt()

        if (danceId < 0) danceId = 1
        if (danceId > 4) danceId = 4

        if (danceId > 1 && !habboSession.habboSubscription.validUserSubscription) danceId = 1

        habboSession.roomUser?.dance(danceId)
    }
}