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

package tk.jomp16.habbo.game.room.tasks

import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.room.IRoomTask
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

class RemoveUserFromRoomTask(private val roomUser: RoomUser) : IRoomTask {
    override fun executeTask(room: Room) {
        room.roomGamemap.removeRoomUser(roomUser, roomUser.currentVector3.vector2)
        room.roomUsers.remove(roomUser.virtualID)

        // todo: item handling
        // todo: trade

        room.sendHabboResponse(Outgoing.ROOM_USER_REMOVE, roomUser.virtualID)
    }
}