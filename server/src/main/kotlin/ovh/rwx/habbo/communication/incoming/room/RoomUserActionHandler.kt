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

package ovh.rwx.habbo.communication.incoming.room

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.game.room.tasks.UserAction
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUserActionHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Handler(Incoming.ROOM_USER_ACTION)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null) return
        val action = habboRequest.readInt()

        if (action > 7) {
            log.error("Unmmaped action: $action")
        }

        habboSession.roomUser?.action(UserAction.fromValue(action))
    }

    @HandlerR63A(IncomingR63A.ROOM_USER_WAVE)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null) return

        habboSession.roomUser?.action(UserAction.WAVE)
    }
}