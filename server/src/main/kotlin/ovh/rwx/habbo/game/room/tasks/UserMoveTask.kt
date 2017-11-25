/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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

import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.IRoomTask
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.util.Vector2

class UserMoveTask(
        private val roomUser: RoomUser,
        private val objectiveVector2: Vector2,
        private val rotation: Int,
        private val actingItem: RoomItem?,
        private val ignoreBlocking: Boolean,
        private val rollerId: Int
) : IRoomTask {
    override fun executeTask(room: Room) {
        roomUser.idle = false

        roomUser.path = mutableListOf()
        roomUser.ignoreBlocking = ignoreBlocking
        roomUser.rollerId = rollerId
        roomUser.objectiveVector2 = objectiveVector2
        roomUser.objectiveRotation = rotation
        roomUser.objectiveItem = actingItem
    }
}