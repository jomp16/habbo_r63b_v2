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
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.pathfinding.core.Grid
import ovh.rwx.habbo.util.Utils
import ovh.rwx.habbo.util.Vector2
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class RoomGamemap(private val room: Room) {
    val blockedItem: Array<BooleanArray> = Array(room.roomModel.mapSizeX) { BooleanArray(room.roomModel.mapSizeY) }
    val cannotStackItem: Array<BooleanArray> = Array(room.roomModel.mapSizeX) { BooleanArray(room.roomModel.mapSizeY) }

    private val roomEntityMap: MutableMap<Vector2, MutableSet<RoomEntity>> = ConcurrentHashMap()
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
            val usersOnTile = roomEntityMap[vector2]
            return !usersOnTile.isNullOrEmpty()
        }

        return false
    }

    fun tileDistance(x1: Int, y1: Int, x2: Int, y2: Int) = abs(x1 - x2) + abs(y1 - y2)

    fun addRoomEntity(roomEntity: RoomEntity, vector2: Vector2) {
        roomEntityMap.getOrPut(vector2) { ConcurrentHashMap.newKeySet() }.add(roomEntity)
    }

    fun removeRoomEntity(roomEntity: RoomEntity, vector2: Vector2) {
        val usersOnTile = roomEntityMap[vector2]
        if (usersOnTile != null) {
            usersOnTile.remove(roomEntity)
            // Evita o vazamento de memória removendo a chave se o Set ficar vazio
            if (usersOnTile.isEmpty()) {
                roomEntityMap.remove(vector2)
            }
        }
    }

    fun updateRoomEntityMovement(roomEntity: RoomEntity, oldVector2: Vector2, newVector2: Vector2) {
        removeRoomEntity(roomEntity, oldVector2)
        addRoomEntity(roomEntity, newVector2)
    }

    fun addRoomItem(roomItem: RoomItem) {
        if (roomItem.furnishing.type != ItemType.FLOOR) return
        roomItem.affectedTiles.forEach {
            setRoomItem(it, roomItem)
        }
    }

    private fun setRoomItem(vector2: Vector2, roomItem: RoomItem) {
        if (roomItem.furnishing.type != ItemType.FLOOR) return

        val itemsOnTile = roomItemMap.getOrPut(vector2) { ConcurrentHashMap.newKeySet() }
        itemsOnTile.removeIf { it.id == roomItem.id }
        itemsOnTile.add(roomItem)
        recomputeTile(vector2)
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
        if (!grid.isInside(x, y)) return 0.0
        val floorHeight = room.roomModel.floorHeight[x][y].toDouble()
        val itemsOnTile = roomItemMap[Vector2(x, y)]

        if (itemsOnTile.isNullOrEmpty()) {
            return Utils.round(floorHeight, 2)
        }

        var highestStack = floorHeight
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

        val finalHeight = (highestStack - deduction).coerceAtLeast(floorHeight)
        return Utils.round(finalHeight, 2)
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

        // Limpeza de segurança em todos os tiles
        roomItemMap.values.forEach { set ->
            set.removeIf { it.id == roomItem.id }
        }
        roomItemMap.entries.removeIf { it.value.isEmpty() }
    }

    private fun unsetRoomItem(vector2: Vector2, roomItem: RoomItem) {
        val itemsOnTile = roomItemMap[vector2] ?: return
        itemsOnTile.removeIf { it.id == roomItem.id }
        if (itemsOnTile.isEmpty()) {
            roomItemMap.remove(vector2)
        }
        recomputeTile(vector2)
    }

    fun recomputeTile(vector2: Vector2) {
        val x = vector2.x
        val y = vector2.y
        if (!grid.isInside(x, y)) return

        val itemsOnTile = roomItemMap[vector2]
        if (itemsOnTile.isNullOrEmpty()) {
            cannotStackItem[x][y] = false
            blockedItem[x][y] = false
            return
        }

        val highestItem = getHighestItem(vector2)
        cannotStackItem[x][y] =
            if (highestItem != null) !highestItem.furnishing.canStack else itemsOnTile.any { !it.furnishing.canStack }
        blockedItem[x][y] = itemsOnTile.any { isItemBlockingPath(it, x, y) }
    }

    fun getEntitiesFromVector2(vector2: Vector2): Set<RoomEntity> = roomEntityMap[vector2] ?: emptySet()

    fun getItemsFromVector2(vector2: Vector2): Set<RoomItem> = roomItemMap[vector2] ?: emptySet()

    fun clearEntities() {
        roomEntityMap.clear()
    }
}
