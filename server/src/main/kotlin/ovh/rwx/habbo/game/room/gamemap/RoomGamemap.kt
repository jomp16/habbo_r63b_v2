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

package ovh.rwx.habbo.game.room.gamemap

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.model.SquareState
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.util.Utils
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.utils.pathfinding.core.Grid
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class RoomGamemap(private val room: Room) {
    val blockedItem: Array<BooleanArray> = Array(room.roomModel.mapSizeX) { BooleanArray(room.roomModel.mapSizeY) }
    val cannotStackItem: Array<BooleanArray> = Array(room.roomModel.mapSizeX) { BooleanArray(room.roomModel.mapSizeY) }
    val roomUserMap: MutableMap<Vector2, MutableSet<RoomUser>> = ConcurrentHashMap()
    val roomItemMap: MutableMap<Vector2, MutableSet<RoomItem>> = ConcurrentHashMap()
    val grid: Grid = Grid(room.roomModel.mapSizeX, room.roomModel.mapSizeY) { _, x, y, overrideBlocking -> overrideBlocking || !isBlocked(Vector2(x, y)) }

    init {
        room.floorItems.values.forEach { addRoomItem(it) }
    }

    fun isBlocked(vector2: Vector2, ignoreUsers: Boolean = false): Boolean {
        if (!grid.isInside(vector2.x, vector2.y)) return true
        if (room.roomModel.doorVector3.x == vector2.x && room.roomModel.doorVector3.y == vector2.y) return false
        if (room.roomModel.squareStates[vector2.x][vector2.y] == SquareState.CLOSED) return true
        if (blockedItem[vector2.x][vector2.y]) return true

        return if (!ignoreUsers && !room.roomData.allowWalkThrough) roomUserMap[vector2] != null && roomUserMap[vector2]!!.isNotEmpty() else false
    }

    fun tileDistance(x1: Int, y1: Int, x2: Int, y2: Int) = Math.abs(x1 - x2) + Math.abs(y1 - y2)

    fun addRoomUser(roomUser: RoomUser, vector2: Vector2) {
        if (!roomUserMap.containsKey(vector2)) {
            val roomUsers: MutableSet<RoomUser> = CopyOnWriteArraySet()
            roomUsers.add(roomUser)

            roomUserMap[vector2] = roomUsers
        } else if (!roomUserMap[vector2]!!.contains(roomUser)) {
            roomUserMap[vector2]?.add(roomUser)
        }
    }

    fun removeRoomUser(roomUser: RoomUser, vector2: Vector2) {
        if (roomUserMap.containsKey(vector2)) roomUserMap[vector2]?.remove(roomUser)
    }

    fun updateRoomUserMovement(roomUser: RoomUser, oldVector2: Vector2, newVector2: Vector2) {
        removeRoomUser(roomUser, oldVector2)
        addRoomUser(roomUser, newVector2)
    }

    fun addRoomItem(roomItem: RoomItem) {
        roomItem.affectedTiles.forEach {
            setRoomItem(it, roomItem)
        }
    }

    private fun setRoomItem(vector2: Vector2, roomItem: RoomItem) {
        if (roomItem.furnishing.type != ItemType.FLOOR) return

        if (!roomItemMap.containsKey(vector2)) roomItemMap[vector2] = CopyOnWriteArraySet()

        if (roomItemMap[vector2]!!.contains(roomItem)) return

        roomItemMap[vector2]?.add(roomItem)

        if (!cannotStackItem[vector2.x][vector2.y]) cannotStackItem[vector2.x][vector2.y] = !roomItem.furnishing.canStack

        if (!blockedItem[vector2.x][vector2.y]) {
            blockedItem[vector2.x][vector2.y] = !roomItem.furnishing.walkable
                    && !(roomItem.position.z <= room.roomModel.floorHeight[vector2.x][vector2.y] + 0.1
                    && roomItem.furnishing.interactionType == InteractionType.GATE
                    && roomItem.extraData == "1")
                    && !(roomItem.furnishing.canSit || roomItem.furnishing.interactionType == InteractionType.BED)
        }
    }

    fun getAbsoluteHeight(vector2: Vector2) = getAbsoluteHeight(vector2.x, vector2.y)

    fun getAbsoluteHeight(x: Int, y: Int): Double {
        val vector2 = Vector2(x, y)

        if (roomItemMap.containsKey(vector2)) {
            var highestStack = 0.toDouble()
            var deduct = false
            var deductable = 0.toDouble()

            roomItemMap[vector2]?.forEach {
                if (it.totalHeight > highestStack) {
                    if (it.furnishing.canSit || it.furnishing.interactionType == InteractionType.BED) {
                        deduct = true
                        deductable = it.height
                    } else {
                        deduct = false
                    }

                    highestStack = it.totalHeight
                }
            }
            val floorHeight = room.roomModel.floorHeight[x][y].toDouble()
            var stackHeight = highestStack - floorHeight

            if (deduct) stackHeight -= deductable
            if (stackHeight < 0) stackHeight = 0.toDouble()

            return Utils.round(floorHeight + stackHeight, 2)
        }

        return Utils.round(room.roomModel.floorHeight[x][y].toDouble(), 2)
    }

    fun getHighestItem(vector2: Vector2): RoomItem? {
        if (!roomItemMap.containsKey(vector2)) return null

        return roomItemMap[vector2]!!.sortedBy { it.position.z }.firstOrNull()
    }

    fun removeRoomItem(roomItem: RoomItem) {
        if (roomItem.furnishing.type == ItemType.WALL) return

        roomItem.affectedTiles.forEach {
            unsetRoomItem(it, roomItem)
        }
    }

    private fun unsetRoomItem(vector2: Vector2, roomItem: RoomItem) {
        if (!roomItemMap.containsKey(vector2) || !roomItemMap[vector2]!!.contains(roomItem)) return

        roomItemMap[vector2]?.remove(roomItem)
        cannotStackItem[vector2.x][vector2.y] = false
        blockedItem[vector2.x][vector2.y] = false

        getHighestItem(vector2)?.let { setRoomItem(vector2, it) }
    }

    fun getUsersFromVector2(vector2: Vector2): Set<RoomUser> = roomUserMap[vector2] ?: emptySet()

    fun clearUsers() {
        roomUserMap.values.forEach { it.clear() }
    }
}