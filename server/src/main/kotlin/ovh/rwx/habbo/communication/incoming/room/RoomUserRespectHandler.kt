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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUserRespectHandler {
    @Handler(Incoming.ROOM_USER_RESPECT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null || habboSession.userStats.dailyRespectPoints <= 0) return
        val targetUser = HabboServer.habboSessionManager.getHabboSessionById(habboRequest.readInt()) ?: return

        if (targetUser.currentRoom != habboSession.currentRoom) return

        habboSession.userStats.dailyRespectPoints--
        targetUser.userStats.respect++

        habboSession.currentRoom?.sendHabboResponse(Outgoing.USER_RESPECT_NOTIFICATION, targetUser.userInformation.id, targetUser.userStats.respect)
        habboSession.roomUser?.action(7)
    }
}