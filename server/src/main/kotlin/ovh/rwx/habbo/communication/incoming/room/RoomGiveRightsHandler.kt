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
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomGiveRightsHandler {
    @Handler(Incoming.ROOM_GIVE_RIGHTS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        this.parse(habboSession, habboRequest)
    }

    @Handler(Incoming.ROOM_GIVE_RIGHTS)
    fun handleWithRoomId(habboSession: HabboSession, habboRequest: HabboRequest) {
        this.parse(habboSession, habboRequest)
    }

    private fun parse(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null || !habboSession.currentRoom!!.hasRights(habboSession, true)) return

        val userId = habboRequest.readInt()

        // Probably something more?

        if (habboSession.currentRoom!!.rights.singleOrNull { it.userId == userId } != null) return

        val userInformation = HabboServer.habboSessionManager.getHabboSessionById(userId)?.userInformation
                ?: UserInformationDao.getUserInformationById(userId)
                ?: return

        // Create rights
        val rightData = RoomDao.addRight(userInformation.id, habboSession.currentRoom!!.roomData.id)

        habboSession.currentRoom!!.rights.add(rightData)

        HabboServer.habboSessionManager.getHabboSessionById(userId)?.let { habboSession1 ->
            if (habboSession1.currentRoom == habboSession.currentRoom) {
                // Update rights
                habboSession1.roomUser!!.addStatus("flatctrl", "1")

                if (habboRequest.methodName == "handle") {
                    habboSession1.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, 1)
                } else if (habboRequest.methodName == "handleWithRoomId") {
                    habboSession1.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, habboSession.currentRoom!!.roomData.id, 1)
                }
            }
        }

        if (habboRequest.methodName == "handle") {
            habboSession.sendHabboResponse(Outgoing.ROOM_RIGHTS_GIVEN, userInformation.id, userInformation.username)
        } else if (habboRequest.methodName == "handleWithRoomId") {
            habboSession.sendHabboResponse(Outgoing.ROOM_RIGHTS_GIVEN, habboSession.currentRoom!!.roomData.id, userInformation.id, userInformation.username)
        }
    }
}