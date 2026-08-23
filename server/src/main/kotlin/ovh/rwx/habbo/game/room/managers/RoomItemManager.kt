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

package ovh.rwx.habbo.game.room.managers

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.database.subscription.SubscriptionDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.dimmer.RoomDimmer
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.room.wired.WiredHandler
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import java.util.concurrent.ConcurrentHashMap

class RoomItemManager(private val room: Room) {
    val items: MutableMap<Int, RoomItem> by lazy { ConcurrentHashMap(ItemDao.getRoomItems(room.roomData.id)) }
    val wallItems: Map<Int, RoomItem> get() = items.filterValues { it.furnishing.type == ItemType.WALL }
    val floorItems: Map<Int, RoomItem> get() = items.filterValues { it.furnishing.type == ItemType.FLOOR }

    val itemsToSave: MutableSet<RoomItem> = ConcurrentHashMap.newKeySet()
    val wiredHandler: WiredHandler by lazy { WiredHandler(room) }
    var roomDimmer: RoomDimmer? = null

    val hasBuildersClubItems: Boolean
        get() = items.values.any { it.buildersClub && it.userId == room.roomData.ownerId }

    val hiddenBuildersClub: Boolean
        get() = hasBuildersClubItems && !SubscriptionDao.hasActiveBuildersClub(room.roomData.ownerId)

    fun loadItems() {
        items.putAll(ItemDao.getRoomItems(room.roomData.id))
    }

    fun triggerItems() {
        items.values.forEach { item ->
            item.furnishing.interactor?.onPlace(room, null, item)

            if (item.furnishing.interactionType.name.startsWith("WIRED_")) {
                HabboServer.habboGame.itemManager.getWiredInstance(room, item)?.let { wired ->
                    wiredHandler.addWiredItem(item.position.vector2, wired)
                }
            }
        }

        roomDimmer = items.values.firstOrNull { it.furnishing.interactionType == InteractionType.DIMMER }?.let {
            ItemDao.getRoomDimmer(it)
        }
    }

    // --- MANIPULAÇÃO DE ITENS DE CHÃO ---
    fun setFloorItem(
        roomItem: RoomItem,
        position: Vector2,
        rotation: Int,
        roomUser: RoomUser?,
        overrideZ: Double = -1.0
    ): Boolean {
        if (position == room.roomModel.doorVector3.vector2) return false
        if (roomItem.position.vector2 == position && roomItem.rotation == rotation && overrideZ == -1.0) return false

        val isNewItem = !items.containsKey(roomItem.id)
        val newAffectedTiles = HabboServer.habboGame.itemManager.getAffectedTiles(
            position.x, position.y, rotation, roomItem.furnishing.width, roomItem.furnishing.length
        )

        if (!canPlaceItemAt(newAffectedTiles, roomItem)) return false

        val tilesToUpdate = mutableSetOf<Vector2>()

        // 1. Desmonta o item da posição antiga (se estiver movendo)
        if (!isNewItem) {
            tilesToUpdate.addAll(roomItem.affectedTiles)
            room.roomGamemap.removeRoomItem(roomItem)

            if (roomItem.furnishing.interactionType.name.startsWith("WIRED_")) {
                wiredHandler.removeWiredItem(roomItem.position.vector2, roomItem)
            }

            // Atualiza usuários que estavam pisando no item velho
            roomItem.affectedTiles.forEach { tile ->
                room.roomGamemap.getEntitiesFromVector2(tile).forEach { user ->
                    roomItem.onEntityWalksOff(user, true)
                    if (tile !in newAffectedTiles) user.removeEntityStatuses()
                    updateUserHeight(user, tile)
                }
            }
        }

        // 2. Atualiza coordenadas físicas do item
        roomItem.position = Vector3(
            position.x,
            position.y,
            if (overrideZ != -1.0) overrideZ else room.roomGamemap.getAbsoluteHeight(position.x, position.y)
        )
        roomItem.rotation = rotation

        // 3. Monta o item na nova posição
        room.roomGamemap.addRoomItem(roomItem)
        tilesToUpdate.addAll(roomItem.affectedTiles)

        if (roomItem.furnishing.interactionType.name.startsWith("WIRED_")) {
            HabboServer.habboGame.itemManager.getWiredInstance(room, roomItem)?.let {
                wiredHandler.addWiredItem(position, it)
            }
        }

        // Atualiza usuários que já estavam naquele tile (ex: colocar cadeira embaixo de alguém)
        roomItem.affectedTiles.forEach { tile ->
            room.roomGamemap.getEntitiesFromVector2(tile).forEach { user ->
                roomItem.onEntityWalksOn(user, true)
                user.addEntityStatuses(roomItem)
                updateUserHeight(user, tile)
            }
        }

        // 4. Dispara Eventos, Salva e Atualiza a Sala
        roomItem.furnishing.interactor?.onPlace(room, roomUser, roomItem)
        saveItemState(roomItem, isNewItem)
        broadcastFloorUpdate(tilesToUpdate)

        return true
    }

    fun removeItem(roomUser: RoomUser?, roomItem: RoomItem): Boolean {
        if (items.remove(roomItem.id) == null) return false

        room.roomGamemap.removeRoomItem(roomItem)
        itemsToSave.remove(roomItem)

        if (roomItem.furnishing.interactionType.name.startsWith("WIRED_")) {
            ItemDao.saveWireds(listOf(roomItem))
            wiredHandler.removeWiredItem(roomItem.position.vector2, roomItem)
        }

        if (roomItem.furnishing.interactionType == InteractionType.DIMMER) {
            roomDimmer?.let { ItemDao.saveDimmer(it) }
            roomDimmer = null
        }

        roomItem.furnishing.interactor?.onRemove(room, roomUser, roomItem)

        if (roomItem.furnishing.type == ItemType.FLOOR) {
            room.networkDispatcher.sendResponseModern(Outgoing.ROOM_FLOOR_ITEM_REMOVE, roomItem, false, 0)
            room.networkDispatcher.sendResponseR63A(OutgoingR63A.ROOM_FLOOR_ITEM_REMOVE, roomItem)

            // Atualiza usuários que caíram do item removido
            roomItem.affectedTiles.forEach { tile ->
                room.roomGamemap.getEntitiesFromVector2(tile).forEach { user ->
                    roomItem.onEntityWalksOff(user, true)
                    user.removeEntityStatuses()
                    updateUserHeight(user, tile)
                }
            }
            broadcastFloorUpdate(roomItem.affectedTiles.toSet())
        } else {
            room.networkDispatcher.sendResponseModern(Outgoing.ROOM_WALL_ITEM_REMOVE, roomItem)
            room.networkDispatcher.sendResponseR63A(OutgoingR63A.ROOM_WALL_ITEM_REMOVE, roomItem)
        }

        return true
    }

    // --- MANIPULAÇÃO DE ITENS DE PAREDE ---
    fun setWallItem(roomItem: RoomItem, wallData: List<String>, roomUser: RoomUser?): Boolean {
        if (wallData.size != 3 || !wallData[0].startsWith(":w=") || !wallData[1].startsWith("l=") || (wallData[2] != "r" && wallData[2] != "l")) return false

        val isNewItem = !items.containsKey(roomItem.id)
        val wBit = wallData[0].substring(3)
        val lBit = wallData[1].substring(2)

        if (!wBit.contains(',') || !lBit.contains(',')) return false

        val (w1, w2) = wBit.split(',').map { it.toInt() }
        val (l1, l2) = lBit.split(',').map { it.toInt() }

        if (listOf(w1, w2, l1, l2).any { it !in 0..200 }) return false

        roomItem.wallPosition = ":w=$w1,$w2 l=$l1,$l2 ${wallData[2]}"

        roomItem.furnishing.interactor?.onPlace(room, roomUser, roomItem)

        if (isNewItem) {
            if (roomItem.furnishing.interactionType == InteractionType.DIMMER) {
                if (roomDimmer != null) return false
                roomDimmer = ItemDao.getRoomDimmer(roomItem)
                roomItem.extraData = roomDimmer!!.generateExtraData()
            }
            saveItemState(roomItem, true)
        } else {
            roomItem.update(updateDb = true, updateClient = true)
        }

        return true
    }

    // --- UTILITÁRIOS INTERNOS ---
    private fun canPlaceItemAt(newAffectedTiles: List<Vector2>, roomItem: RoomItem): Boolean {
        return newAffectedTiles.none { tile ->
            room.roomGamemap.isBlocked(tile, ignoreUsers = true) &&
                    room.roomGamemap.cannotStackItem[tile.x][tile.y] &&
                    room.roomGamemap.getItemsFromVector2(tile).none { it.id == roomItem.id }
        }
    }

    private fun updateUserHeight(user: RoomEntity, tile: Vector2) {
        user.currentVector3 = Vector3(tile, room.roomGamemap.getAbsoluteHeight(tile))
        user.updateNeeded = true
    }

    private fun saveItemState(roomItem: RoomItem, isNew: Boolean) {
        if (isNew) {
            items[roomItem.id] = roomItem
            val ownerName = UserInformationDao.getUserInformationById(roomItem.userId)?.username ?: "No owner name"
            roomItem.addToRoom(room, updateDb = true, updateClient = true, userName = ownerName)
        } else {
            roomItem.update(updateDb = true, updateClient = true)
        }
    }

    private fun broadcastFloorUpdate(affectedTiles: Set<Vector2>) {
        if (affectedTiles.isEmpty()) return

        room.networkDispatcher.sendResponseModern(Outgoing.ROOM_UPDATE_FURNI_STACK, room, affectedTiles)
        val validKeys = room.roomGamemap.roomItemMap.filterValues { it.isNotEmpty() }.keys

        room.userManager.usersWithRights
            .filterIsInstance<RoomUser>()
            .filter { it.habboSession.release != "R63A" }
            .forEach { user ->
                user.habboSession.sendHabboResponse(Outgoing.FLOOR_PLAN_USED_SQUARES, validKeys)
            }
    }

    // --- SALVAMENTO EM LOTE ---
    fun addItemToSave(roomItem: RoomItem) {
        itemsToSave.add(roomItem)
    }

    fun savePendingItems() {
        if (itemsToSave.isEmpty()) return

        RoomDao.saveItems(room.roomData.id, itemsToSave)

        val wiredsToSave =
            itemsToSave.filter { it.furnishing.interactionType.name.startsWith("WIRED_") && it.wiredData != null }
        if (wiredsToSave.isNotEmpty()) ItemDao.saveWireds(wiredsToSave)

        roomDimmer?.let {
            if (itemsToSave.contains(it.roomItem)) ItemDao.saveDimmer(it)
        }

        itemsToSave.clear()
    }
}