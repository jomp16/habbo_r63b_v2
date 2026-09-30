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

package ovh.rwx.habbo.game.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.misc.MiscGenericErrorResponse
import ovh.rwx.habbo.communication.outgoing.misc.MiscSuperNotificationResponse
import ovh.rwx.habbo.communication.outgoing.room.RoomDoorbellDeniedData
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomState
import ovh.rwx.habbo.game.room.user.RoomUser

fun HabboSession.enterRoomImpl(room: Room, password: String = "", bypassAuth: Boolean = false) {
    if (!bypassAuth && room == currentRoom) return

    currentRoom?.userManager?.removeEntity(roomUser, notifyClient = false, kickNotification = false)

    if (room.itemManager.hiddenBuildersClub && userInformation.id != room.roomData.ownerId) {
        if (release != "R63A") {
            sendHabboResponse(Outgoing.ROOM_EXIT)
            sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.BUILDERS_CLUB_VISIT_DENIED_GUEST)
        } else {
            sendHabboResponse(OutgoingR63A.ROOM_EXIT)
            sendNotification($$"${notification.builders_club.visit_denied_for_visitor.message}")
        }
        return
    }

    if (room.isBanned(userInformation.id) && !room.userManager.hasRights(this, true) && !hasPermission("acc_any_room_owner")) {
        if (release != "R63A") {
            sendHabboResponse(Outgoing.ROOM_ERROR, 4, "")
            sendHabboResponse(Outgoing.ROOM_EXIT)
        } else {
            sendHabboResponse(OutgoingR63A.ROOM_ERROR, 4)
            sendHabboResponse(OutgoingR63A.ROOM_EXIT)
        }
        return
    }

    if (!room.running) HabboServer.habboGame.roomManager.roomTaskManager.addRoom(room)

    if (room.userManager.entities.values.filterIsInstance<RoomUser>().size >= room.roomData.usersMax && !room.userManager.hasRights(
            this,
            true
        ) && !hasPermission("acc_enter_full_room")
    ) {
        if (release != "R63A") {
            sendHabboResponse(Outgoing.ROOM_ERROR, 1, "")
            sendHabboResponse(Outgoing.ROOM_EXIT)
        } else {
            sendHabboResponse(OutgoingR63A.ROOM_ERROR, 1)
            sendHabboResponse(OutgoingR63A.ROOM_EXIT)
        }
        return
    }

    val loading = !bypassAuth && !room.userManager.hasRights(this, true)

    if (loading) {
        if (room.roomData.state == RoomState.PASSWORD && !HabboServer.habboGame.passwordEncryptor.checkPassword(
                password,
                room.roomData.password
            )
        ) {
            if (release != "R63A") {
                sendHabboResponse(
                    Outgoing.MISC_GENERIC_ERROR,
                    MiscGenericErrorResponse.MiscGenericError.WRONG_PASSWORD
                )
                sendHabboResponse(Outgoing.ROOM_EXIT)
            } else {
                sendHabboResponse(
                    OutgoingR63A.MISC_GENERIC_ERROR,
                    MiscGenericErrorResponse.MiscGenericError.WRONG_PASSWORD
                )
                sendHabboResponse(OutgoingR63A.ROOM_EXIT)
            }
            return
        } else if (room.roomData.state == RoomState.LOCKED) {
            val roomUsersWithRights = room.userManager.usersWithRights

            if (roomUsersWithRights.isEmpty()) {
                sendHabboResponse(
                    Outgoing.ROOM_DOORBELL_DENIED,
                    RoomDoorbellDeniedData(username = "", roomId = room.roomData.id)
                )

                if (release != "R63A") {
                    sendHabboResponse(Outgoing.ROOM_EXIT)
                } else {
                    sendHabboResponse(OutgoingR63A.ROOM_EXIT)
                }
            } else {
                currentRoom = room

                roomUsersWithRights.forEach {
                    (it as? RoomUser)?.habboSession?.let { habboSession ->
                        if (habboSession.release != "R63A") {
                            habboSession.sendHabboResponse(Outgoing.ROOM_DOORBELL, userInformation.username)
                        } else {
                            habboSession.sendHabboResponse(OutgoingR63A.ROOM_DOORBELL, userInformation.username)
                        }
                    }
                }

                if (release != "R63A") {
                    sendHabboResponse(Outgoing.ROOM_DOORBELL, "")
                } else {
                    sendHabboResponse(OutgoingR63A.ROOM_DOORBELL, "")
                }
            }
            return
        }
    }

    userStats.roomVisits++
    currentRoom = room

    userStats.favoriteGroup?.let {
        room.loadedGroups.add(it)
        room.sendResponse(Outgoing.ROOM_GROUP_BADGES, OutgoingR63A.ROOM_GROUPS_BADGES, room.loadedGroups)
    }

    room.userManager.addUser(this)
}
