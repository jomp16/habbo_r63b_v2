/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.game.room.tasks

import tk.jomp16.habbo.communication.QueuedHabboResponse
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.room.IRoomTask
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

class UserJoinRoomTask(private val roomUser: RoomUser) : IRoomTask {
    override fun executeTask(room: Room) {
        if (room.roomUsers.containsValue(roomUser)) return

        val queuedHabboResponse = QueuedHabboResponse()

        room.roomUsers += roomUser.virtualID to roomUser
        room.roomGamemap.addRoomUser(roomUser, roomUser.currentVector3.vector2)

        roomUser.habboSession?.let {
            it.roomUser = roomUser

            queuedHabboResponse += Outgoing.ROOM_HEIGHTMAP to arrayOf(room)
            queuedHabboResponse += Outgoing.ROOM_FLOORMAP to arrayOf(room)
            queuedHabboResponse += Outgoing.ROOM_USERS to arrayOf(room.roomUsers.values)
            queuedHabboResponse += Outgoing.ROOM_USERS_STATUSES to arrayOf(room.roomUsers.values)
            queuedHabboResponse += Outgoing.ROOM_FLOOR_ITEMS to arrayOf(room, room.floorItems.values)
            queuedHabboResponse += Outgoing.ROOM_WALL_ITEMS to arrayOf(room, room.wallItems.values)
            queuedHabboResponse += Outgoing.ROOM_OWNERSHIP to arrayOf(room.roomData.id, room.hasRights(it, true))
            queuedHabboResponse += Outgoing.ROOM_VISUALIZATION_THICKNESS to arrayOf(room.roomData.hideWall, room.roomData.wallThick, room.roomData.floorThick)

            room.roomUsers.values.forEach {
                if (it.idle) queuedHabboResponse += Outgoing.ROOM_USER_IDLE to arrayOf(it.virtualID, true)
                if (it.danceId > 0) queuedHabboResponse += Outgoing.ROOM_USER_DANCE to arrayOf(it.virtualID, it.danceId)
                // todo: carry item
            }

            // todo: events

            if (room.hasRights(it)) {
                if (room.hasRights(it, true)) {
                    roomUser.addStatus("flatctrl", "useradmin")

                    queuedHabboResponse += Outgoing.ROOM_OWNER to arrayOf()
                    queuedHabboResponse += Outgoing.ROOM_RIGHT_LEVEL to arrayOf(4)
                } else {
                    // todo: add group rights
                    roomUser.addStatus("flatctrl", "1")

                    queuedHabboResponse += Outgoing.ROOM_RIGHT_LEVEL to arrayOf(1)
                }
            } else {
                queuedHabboResponse += Outgoing.ROOM_NO_RIGHTS to arrayOf()
            }

            it.sendQueuedHabboResponse(queuedHabboResponse)

            it.habboMessenger.notifyFriends()

            // todo: wired
        }

        // todo: add support to bots
        roomUser.habboSession?.let {
            room.sendHabboResponse(Outgoing.ROOM_USERS, listOf(roomUser))
            room.sendHabboResponse(Outgoing.USER_UPDATE, roomUser.virtualID, it.userInformation.figure, it.userInformation.gender, it.userInformation.motto, it.userStats.achievementScore)
            room.sendHabboResponse(Outgoing.ROOM_USERS_STATUSES, listOf(roomUser))
        }

        // Reset empty counter
        if (room.emptyCounter.get() > 0) room.emptyCounter.set(0)
    }
}