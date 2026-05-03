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

package ovh.rwx.habbo.game.room.tasks

import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.IRoomTask
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomHumanoid
import ovh.rwx.habbo.util.Vector2

class UserMoveTask(
    private val roomEntity: RoomEntity,
        private val objectiveVector2: Vector2,
        private val rotation: Int,
        private val actingItem: RoomItem?,
        private val ignoreBlocking: Boolean,
        private val rollerId: Int
) : IRoomTask {
    override fun executeTask(room: Room) {
        (roomEntity as? RoomHumanoid)?.idle = false

        roomEntity.path = mutableListOf()
        roomEntity.ignoreBlocking = ignoreBlocking
        roomEntity.rollerId = rollerId
        roomEntity.objectiveVector2 = objectiveVector2
        roomEntity.objectiveRotation = rotation
        roomEntity.objectiveItem = actingItem
    }
}
