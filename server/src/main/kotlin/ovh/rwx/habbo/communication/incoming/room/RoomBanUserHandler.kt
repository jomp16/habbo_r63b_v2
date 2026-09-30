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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomBanUserHandler {
    @Handler(Incoming.ROOM_BAN_USER)
    @HandlerR63A(IncomingR63A.ROOM_BAN_USER)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val isR63A = habboSession.release == "R63A"
        val userId = habboRequest.readInt()
        val roomId = if (isR63A) (habboSession.currentRoom?.roomData?.id ?: return) else habboRequest.readInt()
        val banDuration = if (isR63A) "RWUAM_BAN_USER_PERM" else habboRequest.readUTF()

        val room = HabboServer.habboGame.roomManager.rooms[roomId] ?: habboSession.currentRoom ?: return

        val isOwner = room.roomData.ownerId == habboSession.userInformation.id || habboSession.hasPermission("acc_any_room_owner")
        val canBan = isOwner || (room.roomData.banSettings == 1 && room.userManager.hasRights(habboSession, false))
        if (!canBan) return

        // Cannot ban room owner
        if (userId == room.roomData.ownerId) return

        val targetSession = HabboServer.habboSessionManager.getHabboSessionById(userId)
        if (targetSession != null && targetSession.hasPermission("acc_any_room_owner")) return

        val userInformation = targetSession?.userInformation
            ?: UserInformationDao.getUserInformationById(userId)
            ?: return

        val durationSeconds = when (banDuration) {
            "RWUAM_BAN_USER_HOUR" -> 3600L
            "RWUAM_BAN_USER_DAY" -> 86400L
            "RWUAM_BAN_USER_PERM" -> Long.MAX_VALUE
            else -> 3600L
        }

        room.banUser(userId, userInformation.username, durationSeconds)

        val targetRoomUser = room.userManager.entities.values
            .filterIsInstance<RoomUser>()
            .find { it.habboSession.userInformation.id == userId }
        if (targetRoomUser != null) {
            room.userManager.removeEntity(targetRoomUser, notifyClient = true, kickNotification = true)
        }
    }
}
