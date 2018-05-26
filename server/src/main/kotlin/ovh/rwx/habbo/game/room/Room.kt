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

package ovh.rwx.habbo.game.room

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.misc.MiscGenericErrorResponse
import ovh.rwx.habbo.database.group.GroupDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.game.group.Group
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredItem
import ovh.rwx.habbo.game.room.dimmer.RoomDimmer
import ovh.rwx.habbo.game.room.gamemap.RoomGamemap
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.game.room.tasks.UserJoinRoomTask
import ovh.rwx.habbo.game.room.tasks.UserPartRoomTask
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.room.wired.WiredHandler
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.Utils
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import ovh.rwx.utils.pathfinding.IFinder
import ovh.rwx.utils.pathfinding.core.finders.AStarFinder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashSet

class Room(val roomData: RoomData, val roomModel: RoomModel) : IHabboResponseSerialize {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    // for room task
    var roomTask: RoomTask? = null
    val rollerCounter = AtomicInteger()
    val emptyCounter = AtomicInteger()
    val errorsCounter = AtomicInteger()
    val roomItems: MutableMap<Int, RoomItem> by lazy { ConcurrentHashMap(ItemDao.getRoomItems(roomData.id)) }
    val wallItems: Map<Int, RoomItem>
        get() = roomItems.filterValues { it.furnishing.type == ItemType.WALL }
    val floorItems: Map<Int, RoomItem>
        get() = roomItems.filterValues { it.furnishing.type == ItemType.FLOOR }
    val rights: MutableSet<RightData> by lazy { HashSet(RoomDao.getRights(roomData.id)) }
    val wordFilter: MutableSet<String> by lazy { HashSet(RoomDao.getWordFilter(roomData.id)) }
    val roomUsers: MutableMap<Int, RoomUser> by lazy { HashMap<Int, RoomUser>() }
    val roomUsersWithRights: Set<RoomUser>
        get() = roomUsers.values.filter { hasRights(it.habboSession, false) }.toSet()
    val roomGamemap: RoomGamemap by lazy { RoomGamemap(this) }
    val pathfinder: IFinder by lazy { AStarFinder() }
    val wiredHandler: WiredHandler by lazy { WiredHandler() }
    private val roomItemsToSave: MutableSet<RoomItem> by lazy { HashSet<RoomItem>() }
    var roomDimmer: RoomDimmer? = null
    private var initialized: Boolean = false
    val group: Group?
        get() = if (roomData.groupId == 0) null else HabboServer.habboGame.groupManager.groups[roomData.groupId]
    val loadedGroups: MutableSet<Group> by lazy { HashSet<Group>() }

    fun initialize() {
        if (!initialized) {
            roomItems.values.filter { it.furnishing.interactor != null }.forEach {
                it.furnishing.interactor?.onPlace(this, null, it)
            }
            roomItems.values.filter { it.furnishing.interactionType.name.startsWith("WIRED_") }.forEach { roomItem ->
                HabboServer.habboGame.itemManager.getWiredInstance(this, roomItem)?.let {
                    wiredHandler.addWiredItem(roomItem.position.vector2, it)
                }
            }
            roomItems.values.firstOrNull { it.furnishing.interactionType == InteractionType.DIMMER }?.let {
                roomDimmer = ItemDao.getRoomDimmer(it)
            }

            group?.let {
                loadedGroups.add(it)
            }
        }
    }

    fun sendHabboResponse(habboResponse: HabboResponse) {
        // todo: find a way to cache habbo response
        roomUsers.values.forEach { it.habboSession?.sendHabboResponse(habboResponse) }
    }

    fun sendHabboResponse(outgoing: Outgoing, vararg args: Any?) {
        // todo: find a way to cache habbo response
        roomUsers.values.forEach { it.habboSession?.sendHabboResponse(outgoing, *args) }
    }

    fun hasRights(habboSession: HabboSession?, ownerRight: Boolean = false): Boolean {
        if (habboSession == null) return false
        val isOwner = roomData.ownerId == habboSession.userInformation.id || habboSession.hasPermission("acc_any_room_owner")

        if (group != null) {
            group?.let { group ->
                return if (ownerRight) isOwner else isOwner || rights.any { it.userId == habboSession.userInformation.id } ||
                        if (group.groupData.onlyAdminCanDecorateRoom) group.admins.singleOrNull { it.userId == habboSession.userInformation.id } != null || !habboSession.hasPermission("acc_any_group_admin")
                        else group.members.singleOrNull { it.userId == habboSession.userInformation.id } != null
            }
        }

        return if (ownerRight) isOwner else isOwner || rights.any { it.userId == habboSession.userInformation.id }// || group != null && if (group!!.groupData.onlyAdminCanDecorateRoom) group!!.admins.(habboSession.getHabboUserInformation().getId()) else group.getMembers().containsKey(habboSession.getHabboUserInformation().getId());
    }

    fun addUser(habboSession: HabboSession) {
        roomTask?.let {
            if (roomUsers.values.any { it.habboSession == habboSession }) return
            // generate random virtual id
            var virtualId: Int

            do {
                virtualId = Utils.randInt(1..Int.MAX_VALUE)
            } while (roomUsers.containsKey(virtualId))

            log.debug("Assigned virtual ID {} to user {}", virtualId, habboSession.userInformation.username)

            it.addTask(this, UserJoinRoomTask(RoomUser(habboSession, this, virtualId, roomModel.doorVector3, roomModel.doorDir, roomModel.doorDir)))
        }
    }

    fun removeUser(roomUser: RoomUser?, notifyClient: Boolean, kickNotification: Boolean) {
        if (roomUser == null) return

        if (roomUser.habboSession != null) {
            if (kickNotification) roomUser.habboSession.sendHabboResponse(Outgoing.MISC_GENERIC_ERROR, MiscGenericErrorResponse.MiscGenericError.ROOM_KICKED)
            if (notifyClient) roomUser.habboSession.sendHabboResponse(Outgoing.ROOM_EXIT)

            roomUser.habboSession.roomUser = null
            roomUser.habboSession.currentRoom = null
            roomUser.habboSession.habboMessenger.notifyFriends()
        }

        roomGamemap.removeRoomUser(roomUser, roomUser.currentVector3.vector2)
        roomUsers.remove(roomUser.virtualID)

        roomTask?.addTask(this, UserPartRoomTask(roomUser))
    }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            val showEvents = params[0] as Boolean
            val enterRoom = params[1] as Boolean

            writeInt(roomData.id)
            writeUTF(roomData.name)
            writeInt(roomData.ownerId)
            writeUTF(roomData.ownerName)
            writeInt(roomData.state.state)
            writeInt(roomUsers.size)
            writeInt(roomData.usersMax)
            writeUTF(roomData.description)
            writeInt(roomData.tradeState)
            writeInt(roomData.score)
            writeInt(0) // ranking
            writeInt(roomData.category)

            writeInt(roomData.tags.size)

            roomData.tags.forEach { writeUTF(it) }
            var value = if (enterRoom) 32 else 0

            group?.let { value += 2 }

            if (showEvents) {
                // todo: events
                //value += 4;
            }

            if (roomData.roomType == RoomType.PRIVATE) value += 8

            if (roomData.allowPets) value += 16

            writeInt(value)

            group?.let {
                writeInt(it.groupData.id)
                writeUTF(it.groupData.name)
                writeUTF(it.groupData.badge)
            }
        }
    }

    fun addItemToSave(roomItem: RoomItem) {
        if (!roomItemsToSave.contains(roomItem)) roomItemsToSave += roomItem
    }

    fun saveRoom() {
        // Save room data on database
        RoomDao.updateRoomData(roomData)
        // Save group data on database, if it exists
        group?.let { GroupDao.updateGroupData(it.groupData) }

        if (roomItemsToSave.isEmpty()) return

        RoomDao.saveItems(roomData.id, roomItemsToSave)

        roomItemsToSave.filter { it.furnishing.interactionType.name.startsWith("WIRED_") }.filter { it.wiredData != null }.let {
            if (it.isNotEmpty()) ItemDao.saveWireds(it)
        }

        if (roomDimmer != null && roomItemsToSave.any { it == roomDimmer!!.roomItem }) ItemDao.saveDimmer(roomDimmer!!)

        roomItemsToSave.clear()
    }

    fun setFloorItem(roomItem: RoomItem, position: Vector2, rotation: Int, roomUser: RoomUser?, overrideZ: Double = (-1).toDouble(), rollerId: Int = -1): Boolean {
        val newItem = !roomItems.containsKey(roomItem.id)
        val onlyRotation = roomItem.rotation == rotation

        if (roomItem.position.vector2 == position && roomItem.rotation == rotation) return false

        HabboServer.habboGame.itemManager.getAffectedTiles(position.x, position.y, rotation, roomItem.furnishing.width, roomItem.furnishing.height).forEach {
            if (onlyRotation && roomGamemap.cannotStackItem[it.x][it.y] && roomGamemap.isBlocked(it, true) && overrideZ == (-1).toDouble()) {
                // cannot set item, because at least one tile is blocked
                return false
            }
        }
        val affectedTiles = HashSet<Vector2>()
        val wiredItem: WiredItem? = if (roomItem.furnishing.interactionType.name.startsWith("WIRED_")) {
            if (!newItem) wiredHandler.removeWiredItem(roomItem.position.vector2, roomItem)
            else HabboServer.habboGame.itemManager.getWiredInstance(this, roomItem)
        } else null

        if (!newItem) {
            roomGamemap.removeRoomItem(roomItem)

            HabboServer.habboGame.itemManager.getAffectedTiles(roomItem.position.x, roomItem.position.y, roomItem.rotation, roomItem.furnishing.width, roomItem.furnishing.height).let {
                it.forEach { vector2 ->
                    roomGamemap.getUsersFromVector2(vector2).forEach {
                        roomItem.onUserWalksOff(it, true)

                        it.removeUserStatuses()

                        it.currentVector3 = Vector3(vector2, roomGamemap.getAbsoluteHeight(vector2))
                        it.updateNeeded = true
                    }
                }

                affectedTiles += it
            }

            roomItem.furnishing.interactor?.onRemove(this, roomUser, roomItem)
        }
        val oldPosition = roomItem.position

        roomItem.position = Vector3(position.x, position.y, if (overrideZ != (-1).toDouble()) overrideZ else roomGamemap.getAbsoluteHeight(position.x, position.y))
        roomItem.rotation = rotation

        roomGamemap.addRoomItem(roomItem)

        roomItem.affectedTiles.let {
            it.forEach { vector2 ->
                roomGamemap.getUsersFromVector2(vector2).forEach {
                    roomItem.onUserWalksOn(it, true)

                    it.addUserStatuses(roomItem)

                    it.currentVector3 = Vector3(vector2, roomGamemap.getAbsoluteHeight(vector2))
                    it.updateNeeded = true
                }
            }

            affectedTiles += it
        }

        if (wiredItem != null) wiredHandler.addWiredItem(position, wiredItem)

        roomItem.furnishing.interactor?.onPlace(this, roomUser, roomItem)

        if (newItem) {
            roomItems[roomItem.id] = roomItem

            roomItem.addToRoom(this, true, true, roomUser?.habboSession?.userInformation?.username ?: "")
        } else {
            if (rollerId == -1) {
                roomItem.update(true, true)
            } else {
                sendHabboResponse(Outgoing.ROOM_ROLLER, oldPosition, roomItem.position, -1, rollerId, roomItem.id)

                addItemToSave(roomItem)
            }
        }

        sendHabboResponse(Outgoing.ROOM_UPDATE_FURNI_STACK, this, affectedTiles)

        return true
    }

    fun setWallItem(roomItem: RoomItem, wallData: List<String>, roomUser: RoomUser?): Boolean {
        if (wallData.size != 3 || !wallData[0].startsWith(":w=") || !wallData[1].startsWith("l=") || wallData[2] != "r" && wallData[2] != "l") return false
        val newItem = !roomItems.containsKey(roomItem.id)
        val wBit = wallData[0].substring(3, wallData[0].length)
        val lBit = wallData[1].substring(2, wallData[1].length)

        if (!wBit.contains(',') || !lBit.contains(',')) return false
        val wBitSplit = wBit.split(',')
        val lBitSplit = lBit.split(',')
        val w1 = wBitSplit[0].toInt()
        val w2 = wBitSplit[1].toInt()
        val l1 = lBitSplit[0].toInt()
        val l2 = lBitSplit[1].toInt()

        if (w1 < 0 || w2 < 0 || l1 < 0 || l2 < 0 || w1 > 200 || w2 > 200 || l1 > 200 || l2 > 200) return false

        roomItem.wallPosition = ":w=$w1,$w2 l=$l1,$l2 ${wallData[2]}"

        roomItem.furnishing.interactor?.onPlace(this, roomUser, roomItem)

        if (newItem) {
            if (roomItem.furnishing.interactionType == InteractionType.DIMMER) {
                if (roomDimmer != null) return false

                roomDimmer = ItemDao.getRoomDimmer(roomItem)
                roomItem.extraData = roomDimmer!!.generateExtraData()
            }

            roomItems[roomItem.id] = roomItem

            roomItem.addToRoom(this, true, true, roomUser?.habboSession?.userInformation?.username ?: "")
        } else {
            roomItem.update(true, true)
        }

        return true
    }

    fun removeItem(roomUser: RoomUser?, roomItem: RoomItem): Boolean {
        if (!roomItems.containsValue(roomItem)) return false

        roomItems.remove(roomItem.id)
        roomGamemap.removeRoomItem(roomItem)

        if (roomItem.furnishing.interactionType.name.startsWith("WIRED_") && roomItem.wiredData != null) {
            ItemDao.saveWireds(listOf(roomItem))

            wiredHandler.removeWiredItem(roomItem.position.vector2, roomItem)
        }

        if (roomItem.furnishing.interactionType == InteractionType.DIMMER) {
            ItemDao.saveDimmer(roomDimmer!!)

            roomDimmer = null
        }

        roomItem.furnishing.interactor?.onRemove(this, roomUser, roomItem)

        @Suppress("NON_EXHAUSTIVE_WHEN") when (roomItem.furnishing.type) {
            ItemType.FLOOR -> {
                sendHabboResponse(Outgoing.ROOM_FLOOR_ITEM_REMOVE, roomItem, false, 0)

                HabboServer.habboGame.itemManager.getAffectedTiles(roomItem.position.x, roomItem.position.y, roomItem.rotation, roomItem.furnishing.width, roomItem.furnishing.height).let {
                    it.forEach { vector2 ->
                        roomGamemap.getUsersFromVector2(vector2).forEach {
                            roomItem.onUserWalksOff(it, true)

                            it.removeUserStatuses()

                            it.currentVector3 = Vector3(vector2, roomGamemap.getAbsoluteHeight(vector2))
                            it.updateNeeded = true
                        }
                    }

                    sendHabboResponse(Outgoing.ROOM_UPDATE_FURNI_STACK, this, it)
                }
            }
            ItemType.WALL -> {
                sendHabboResponse(Outgoing.ROOM_WALL_ITEM_REMOVE, roomItem)
            }
        }

        if (roomItemsToSave.contains(roomItem)) roomItemsToSave.remove(roomItem)

        return true
    }

    fun updateGroupInfo() {
        group?.let { group ->
            roomUsers.values.filter { it.habboSession != null }.forEach {
                it.habboSession?.let {
                    it.sendHabboResponse(Outgoing.GROUP_INFO, it.userInformation.id, it.userStats.favoriteGroupId == group.groupData.id, group, false)
                }
            }
        }
    }

    fun updateGroupRights() {
        group?.let { group ->
            roomUsers.values.filter { it.habboSession != null }.filter { it.habboSession?.userInformation?.id != group.groupData.ownerId }.forEach { roomUser ->
                roomUser.habboSession?.let { habboSession ->
                    val methodName = HabboServer.habboHandler.outgoingHeaders.find { it.name == "ROOM_OWNER" && (it.release == habboSession.release) }?.overrideMethod
                            ?: "response"

                    when {
                        hasRights(habboSession, false) -> {
                            roomUser.addStatus("flatctrl", "1")

                            when (methodName) {
                                "response" -> habboSession.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, 1)
                                "responseWithRoomId" -> habboSession.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, roomData.id, 1)
                                else -> log.error("Couldn't send response!")
                            }
                        }
                        roomUser.statusMap.containsKey("flatctrl") -> {
                            roomUser.removeStatus("flatctrl")

                            when (methodName) {
                                "response" -> habboSession.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, 0)
                                "responseWithRoomId" -> habboSession.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, roomData.id, 0)
                                else -> log.error("Couldn't send response!")
                            }
                        }
                        else -> log.error("Couldn't send response!")
                    }
                }
            }
        }
    }
}