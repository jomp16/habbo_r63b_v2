/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.room.tasks

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerEnterRoom
import ovh.rwx.habbo.game.room.IRoomTask
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

class UserJoinRoomTask(private val roomUser: RoomUser) : IRoomTask {
    override fun executeTask(room: Room) {
        if (room.roomUsers.containsValue(roomUser)) return

        room.roomUsers[roomUser.virtualID] = roomUser
        room.roomGamemap.addRoomUser(roomUser, roomUser.currentVector3.vector2)

        roomUser.habboSession?.let { habboSession ->
            habboSession.roomUser = roomUser

            if (habboSession.teleporting) {
                room.roomItems[habboSession.targetTeleportId]?.let { teleportItem ->
                    if (!teleportItem.interactingUsers.containsKey(2)) {
                        roomUser.walkingBlocked = true
                        roomUser.currentVector3 = teleportItem.position
                        roomUser.headRotation = teleportItem.rotation
                        roomUser.bodyRotation = teleportItem.rotation

                        teleportItem.interactingUsers[2] = roomUser
                        teleportItem.extraData = "2"
                        teleportItem.update(updateDb = false, updateClient = true)
                        teleportItem.requestCycles(2)
                    }

                    habboSession.targetTeleportId = 0
                }
            }

            habboSession.sendHabboResponse(Outgoing.ROOM_HEIGHTMAP, room)
            habboSession.sendHabboResponse(Outgoing.ROOM_FLOORMAP, room)
            habboSession.sendHabboResponse(Outgoing.ROOM_OWNERSHIP, room.roomData.id, room.hasRights(habboSession, true))
            habboSession.sendHabboResponse(Outgoing.ROOM_VISUALIZATION_THICKNESS, room.roomData.hideWall, room.roomData.wallThick, room.roomData.floorThick)
            // todo: events
            val methodName = HabboServer.habboHandler.getOverrideMethodForHeader(Outgoing.ROOM_OWNER, habboSession.release)

            if (room.hasRights(habboSession)) {
                if (room.hasRights(habboSession, true)) {
                    roomUser.addStatus("flatctrl", "useradmin")

                    if (methodName == "response") {
                        habboSession.sendHabboResponse(Outgoing.ROOM_OWNER)
                        habboSession.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, 4)
                    } else if (methodName == "responseWithRoomId") {
                        habboSession.sendHabboResponse(Outgoing.ROOM_OWNER, room.roomData.id)
                        habboSession.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, room.roomData.id, 4)
                    }
                } else {
                    // todo: add group rights
                    roomUser.addStatus("flatctrl", "1")

                    if (methodName == "response") habboSession.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, 1)
                    else if (methodName == "responseWithRoomId") habboSession.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, room.roomData.id, 1)
                }
            } else {
                if (methodName == "response") {
                    habboSession.sendHabboResponse(Outgoing.ROOM_NO_RIGHTS)
                } else if (methodName == "responseWithRoomId") {
                    habboSession.sendHabboResponse(Outgoing.ROOM_NO_RIGHTS, room.roomData.id)
                }
            }

            habboSession.habboMessenger.notifyFriends()
            // items at end because optimization
            habboSession.sendHabboResponse(Outgoing.ROOM_USERS, room.roomUsers.values)
            habboSession.sendHabboResponse(Outgoing.ROOM_USERS_STATUSES, room.roomUsers.values)

            habboSession.sendHabboResponse(Outgoing.ROOM_FLOOR_ITEMS, room, room.floorItems.values)
            habboSession.sendHabboResponse(Outgoing.ROOM_WALL_ITEMS, room, room.wallItems.values)

            room.roomUsers.values.forEach {
                if (it.idle) habboSession.sendHabboResponse(Outgoing.ROOM_USER_IDLE, it.virtualID, true)
                if (it.danceId > 0) habboSession.sendHabboResponse(Outgoing.ROOM_USER_DANCE, it.virtualID, it.danceId)
                if (it.handItem > 0) habboSession.sendHabboResponse(Outgoing.ROOM_USER_HANDITEM, it.virtualID, it.handItem)
                it.effect?.let { effect -> habboSession.sendHabboResponse(Outgoing.ROOM_USER_EFFECT, it.virtualID, effect.effectId) }
            }

            room.wiredHandler.triggerWired(WiredTriggerEnterRoom::class, roomUser, null)
        }
        // todo: add support to bots
        roomUser.habboSession?.let {
            //            room.sendHabboResponse(Outgoing.ROOM_USERS, listOf(roomUser))
            room.sendHabboResponse(Outgoing.USER_UPDATE, roomUser.virtualID, it.userInformation.figure, it.userInformation.gender, it.userInformation.motto, it.userStats.achievementScore)
            room.sendHabboResponse(Outgoing.ROOM_USERS_STATUSES, listOf(roomUser))
        }
        // Reset empty counter
        if (room.emptyCounter.get() > 0) room.emptyCounter.set(0)
    }
}