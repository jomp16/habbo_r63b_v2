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

package tk.jomp16.habbo.game.room.gamemap

import tk.jomp16.habbo.game.item.InteractionType
import tk.jomp16.habbo.game.item.ItemType
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.model.SquareState
import tk.jomp16.habbo.game.room.user.RoomUser
import tk.jomp16.habbo.util.Vector2
import tk.jomp16.utils.pathfinding.core.Grid
import java.util.concurrent.ConcurrentHashMap

class RoomGamemap(private val room: Room) {
    private val blockedItem: Array<BooleanArray> = Array(room.roomModel.mapSizeX) { BooleanArray(room.roomModel.mapSizeY) }
    val cannotStackItem: Array<BooleanArray> = Array(room.roomModel.mapSizeX) { BooleanArray(room.roomModel.mapSizeY) }
    private val roomUserMap: MutableMap<Vector2, MutableList<RoomUser>> = ConcurrentHashMap()
    private val roomItemMap: MutableMap<Vector2, MutableList<RoomItem>> = ConcurrentHashMap()

    val grid: Grid = Grid(room.roomModel.mapSizeX, room.roomModel.mapSizeY, { grid, x, y -> !isBlocked(Vector2(x, y)) })

    init {
        room.floorItems.values.forEach { addRoomItem(it) }
    }

    fun isBlocked(vector2: Vector2, ignoreUsers: Boolean = false): Boolean {
        if (!grid.isInside(vector2.x, vector2.y)) return true
        if (room.roomModel.doorVector3.x == vector2.x && room.roomModel.doorVector3.y == vector2.y) return false
        if (room.roomModel.squareStates[vector2.x][vector2.y] == SquareState.CLOSED) return true
        if (blockedItem[vector2.x][vector2.y]) return true

        if (!ignoreUsers) {
            if (!room.roomData.allowWalkThrough) return roomUserMap[vector2] != null && roomUserMap[vector2]!!.isNotEmpty()
        }

        return false
    }

    fun tileDistance(x1: Int, y1: Int, x2: Int, y2: Int) = Math.abs(x1 - x2) + Math.abs(y1 - y2)

    fun addRoomUser(roomUser: RoomUser, vector2: Vector2) {
        if (!roomUserMap.containsKey(vector2)) {
            val roomUsers: MutableList<RoomUser> = mutableListOf()
            roomUsers.add(roomUser)

            roomUserMap.put(vector2, roomUsers)
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

        if (!roomItemMap.containsKey(vector2)) roomItemMap.put(vector2, mutableListOf())

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

    fun getAbsoluteHeight(x: Int, y: Int): Double {
        val vector2 = Vector2(x, y)

        if (roomItemMap.containsKey(vector2)) {
            var highestStack = 0.0
            var deduct = false
            var deductable = 0.0

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
            if (stackHeight < 0) stackHeight = 0.0

            return floorHeight + stackHeight
        }

        return room.roomModel.floorHeight[x][y].toDouble()
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

    fun getUsersFromVector2(vector2: Vector2): List<RoomUser> = roomUserMap[vector2] ?: emptyList()
}