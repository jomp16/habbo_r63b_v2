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

package ovh.rwx.habbo.game.room.gamemap

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.model.SquareState
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.pathfinding.core.Grid
import ovh.rwx.habbo.util.Utils
import ovh.rwx.habbo.util.Vector2
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.abs

class RoomGamemap(private val room: Room) {
    val blockedItem: Array<BooleanArray> = Array(room.roomModel.mapSizeX) { BooleanArray(room.roomModel.mapSizeY) }
    val cannotStackItem: Array<BooleanArray> = Array(room.roomModel.mapSizeX) { BooleanArray(room.roomModel.mapSizeY) }

    private val roomUserMap: MutableMap<Vector2, MutableSet<RoomUser>> = ConcurrentHashMap()
    val roomItemMap: MutableMap<Vector2, MutableSet<RoomItem>> = ConcurrentHashMap()

    val grid: Grid = Grid(room.roomModel.mapSizeX, room.roomModel.mapSizeY) { _, x, y, overrideBlocking ->
        !isBlocked(Vector2(x, y), overrideBlocking = overrideBlocking)
    }

    init {
        room.itemManager.floorItems.values.forEach { addRoomItem(it) }
    }

    fun isBlocked(vector2: Vector2, ignoreUsers: Boolean = false, overrideBlocking: Boolean = false): Boolean {
        if (!grid.isInside(vector2.x, vector2.y)) return true
        if (overrideBlocking) return false
        if (room.roomModel.doorVector3.x == vector2.x && room.roomModel.doorVector3.y == vector2.y) return false
        if (room.roomModel.squareStates[vector2.x][vector2.y] == SquareState.CLOSED) return true
        if (blockedItem[vector2.x][vector2.y]) return true

        if (!ignoreUsers && !room.roomData.allowWalkThrough) {
            val usersOnTile = roomUserMap[vector2]
            return !usersOnTile.isNullOrEmpty()
        }

        return false
    }

    fun tileDistance(x1: Int, y1: Int, x2: Int, y2: Int) = abs(x1 - x2) + abs(y1 - y2)

    fun addRoomUser(roomUser: RoomUser, vector2: Vector2) {
        roomUserMap.getOrPut(vector2) { CopyOnWriteArraySet() }.add(roomUser)
    }

    fun removeRoomUser(roomUser: RoomUser, vector2: Vector2) {
        val usersOnTile = roomUserMap[vector2]
        if (usersOnTile != null) {
            usersOnTile.remove(roomUser)
            // Evita o vazamento de memória removendo a chave se o Set ficar vazio
            if (usersOnTile.isEmpty()) {
                roomUserMap.remove(vector2)
            }
        }
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

        val itemsOnTile = roomItemMap.getOrPut(vector2) { CopyOnWriteArraySet() }
        if (!itemsOnTile.add(roomItem)) return // Se já contém, encerra.

        val x = vector2.x
        val y = vector2.y

        if (!cannotStackItem[x][y]) {
            cannotStackItem[x][y] = !roomItem.furnishing.canStack
        }

        if (!blockedItem[x][y]) {
            blockedItem[x][y] = isItemBlockingPath(roomItem, x, y)
        }
    }

    private fun isItemBlockingPath(item: RoomItem, x: Int, y: Int): Boolean {
        if (item.furnishing.walkable) return false
        if (item.furnishing.canSit || item.furnishing.interactionType == InteractionType.BED) return false

        val isGateOpen = item.furnishing.interactionType == InteractionType.GATE &&
                item.extraData == "1" &&
                item.position.z <= room.roomModel.floorHeight[x][y] + 0.1

        return !isGateOpen
    }

    fun getAbsoluteHeight(vector2: Vector2) = getAbsoluteHeight(vector2.x, vector2.y)

    fun getAbsoluteHeight(x: Int, y: Int): Double {
        val floorHeight = room.roomModel.floorHeight[x][y].toDouble()
        val itemsOnTile = roomItemMap[Vector2(x, y)]

        if (itemsOnTile.isNullOrEmpty()) {
            return Utils.round(floorHeight, 2)
        }

        var highestStack = 0.0
        var deduction = 0.0

        itemsOnTile.forEach {
            if (it.totalHeight > highestStack) {
                highestStack = it.totalHeight

                deduction = if (it.furnishing.canSit || it.furnishing.interactionType == InteractionType.BED) {
                    it.height
                } else {
                    0.0
                }
            }
        }

        var stackHeight = highestStack - floorHeight - deduction
        if (stackHeight < 0) stackHeight = 0.0

        return Utils.round(floorHeight + stackHeight, 2)
    }

    fun getHighestItem(vector2: Vector2): RoomItem? {
        val items = roomItemMap[vector2]
        if (items.isNullOrEmpty()) return null

        return items.maxWithOrNull(
            compareBy<RoomItem> { it.totalHeight }.thenBy { it.id }
        )
    }

    fun removeRoomItem(roomItem: RoomItem) {
        if (roomItem.furnishing.type == ItemType.WALL) return

        roomItem.affectedTiles.forEach {
            unsetRoomItem(it, roomItem)
        }
    }

    private fun unsetRoomItem(vector2: Vector2, roomItem: RoomItem) {
        val itemsOnTile = roomItemMap[vector2] ?: return
        if (!itemsOnTile.remove(roomItem)) return

        val x = vector2.x
        val y = vector2.y

        // Ao remover um item, resetamos os bloqueios do tile temporariamente
        cannotStackItem[x][y] = false
        blockedItem[x][y] = false

        // Reavalia o item mais alto restante para aplicar as regras de bloqueio dele
        getHighestItem(vector2)?.let { setRoomItem(vector2, it) }
    }

    fun getUsersFromVector2(vector2: Vector2): Set<RoomUser> = roomUserMap[vector2] ?: emptySet()

    fun getItemsFromVector2(vector2: Vector2): Set<RoomItem> = roomItemMap[vector2] ?: emptySet()

    fun clearUsers() {
        roomUserMap.clear()
    }
}
