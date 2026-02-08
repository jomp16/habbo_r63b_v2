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

package ovh.rwx.habbo.game.room

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.misc.MiscGenericErrorResponse
import ovh.rwx.habbo.database.group.GroupDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.group.Group
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredItem
import ovh.rwx.habbo.game.room.dimmer.RoomDimmer
import ovh.rwx.habbo.game.room.gamemap.RoomGamemap
import ovh.rwx.habbo.game.room.games.BattleBanzaiGame
import ovh.rwx.habbo.game.room.games.RoomGameManager
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.game.room.tasks.UserJoinRoomTask
import ovh.rwx.habbo.game.room.tasks.UserPartRoomTask
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.room.wired.WiredHandler
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.pathfinding.IFinder
import ovh.rwx.habbo.pathfinding.core.DiagonalMovement
import ovh.rwx.habbo.pathfinding.core.finders.AStarFinder
import ovh.rwx.habbo.pathfinding.core.heuristics.EuclideanHeuristic
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class Room(val roomData: RoomData, var roomModel: RoomModel) : IHabboResponseSerialize {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    // for room task
    var roomTask: RoomTask? = null
    val rollerCounter = AtomicInteger()
    val emptyCounter = AtomicInteger()
    val errorsCounter = AtomicInteger()
    val roomTimer = AtomicInteger()
    val hostingCounter = AtomicInteger()
    val roomItems: MutableMap<Int, RoomItem> by lazy { ConcurrentHashMap(ItemDao.getRoomItems(roomData.id)) }
    val wallItems: Map<Int, RoomItem>
        get() = roomItems.filterValues { it.furnishing.type == ItemType.WALL }
    val floorItems: Map<Int, RoomItem>
        get() = roomItems.filterValues { it.furnishing.type == ItemType.FLOOR }
    val rights: MutableSet<RightData> by lazy { HashSet(RoomDao.getRights(roomData.id)) }
    val wordFilter: MutableSet<String> by lazy { HashSet(RoomDao.getWordFilter(roomData.id)) }
    val roomUsers: MutableMap<Int, RoomUser> by lazy { HashMap() }
    val roomUsersWithRights: Set<RoomUser>
        get() = roomUsers.values.filter { hasRights(it.habboSession, false) }.toSet()
    lateinit var roomGamemap: RoomGamemap
    val pathfinder: IFinder by lazy { AStarFinder(DiagonalMovement.ALWAYS, EuclideanHeuristic()) }
    val wiredHandler: WiredHandler by lazy { WiredHandler() }
    val gameManager: RoomGameManager by lazy { RoomGameManager(this) }

    @Suppress("RemoveExplicitTypeArguments")
    private val roomItemsToSave: MutableSet<RoomItem> by lazy { HashSet<RoomItem>() }
    var roomDimmer: RoomDimmer? = null
    private var initialized: Boolean = false
    val group: Group?
        get() = if (roomData.groupId == 0) null else HabboServer.habboGame.groupManager.groups[roomData.groupId]

    @Suppress("RemoveExplicitTypeArguments")
    val loadedGroups: MutableSet<Group> by lazy { HashSet<Group>() }

    fun initialize() {
        if (!initialized) {
            roomGamemap = RoomGamemap(this)

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

            if (roomItems.values.any { it.furnishing.interactionType.name.startsWith("BATTLE_BANZAI") }) {
                gameManager.registerGame("banzai", BattleBanzaiGame(this))
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
        roomUsers.values.filter { it.habboSession?.release != "R63A" }
            .forEach { it.habboSession?.sendHabboResponse(outgoing, *args) }
    }

    fun sendHabboResponse(outgoing: OutgoingR63A, vararg args: Any?) {
        // todo: find a way to cache habbo response
        roomUsers.values.filter { it.habboSession?.release == "R63A" }
            .forEach { it.habboSession?.sendHabboResponse(outgoing, *args) }
    }

    fun hasRights(
        habboSession: HabboSession?,
        ownerRight: Boolean = false,
        ignorePermissionAnyRoomOwner: Boolean = false
    ): Boolean {
        if (habboSession == null) return false
        val isOwner =
            roomData.ownerId == habboSession.userInformation.id || (!ignorePermissionAnyRoomOwner && habboSession.hasPermission(
                "acc_any_room_owner"
            ))

        if (group != null) {
            group?.let { group ->
                return if (ownerRight) isOwner else isOwner || rights.any { it.userId == habboSession.userInformation.id } ||
                        if (group.groupData.onlyAdminCanDecorateRoom) group.admins.singleOrNull { it.userId == habboSession.userInformation.id } != null || !habboSession.hasPermission(
                            "acc_any_group_admin"
                        )
                        else group.members.singleOrNull { it.userId == habboSession.userInformation.id } != null
            }
        }

        return if (ownerRight) isOwner else isOwner || rights.any { it.userId == habboSession.userInformation.id }// || group != null && if (group!!.groupData.onlyAdminCanDecorateRoom) group!!.admins.(habboSession.getHabboUserInformation().getId()) else group.getMembers().containsKey(habboSession.getHabboUserInformation().getId());
    }

    fun addUser(habboSession: HabboSession) {
        roomTask?.let {
            if (roomUsers.values.any { roomUser -> roomUser.habboSession == habboSession }) return
            // generate random virtual id
            var virtualId: Int

            do {
                virtualId = (1..Int.MAX_VALUE).random()
            } while (roomUsers.containsKey(virtualId))

            log.debug("Assigned virtual ID {} to user {}", virtualId, habboSession.userInformation.username)

            it.addTask(
                this,
                UserJoinRoomTask(
                    RoomUser(
                        habboSession,
                        this,
                        virtualId,
                        roomModel.doorVector3,
                        roomModel.doorDir,
                        roomModel.doorDir
                    )
                )
            )
        }
    }

    fun removeUser(roomUser: RoomUser?, notifyClient: Boolean, kickNotification: Boolean) {
        if (roomUser == null) return

        if (roomUser.habboSession != null) {
            if (kickNotification) {
                if (roomUser.habboSession.release != "R63A") {
                    roomUser.habboSession.sendHabboResponse(
                        Outgoing.MISC_GENERIC_ERROR,
                        MiscGenericErrorResponse.MiscGenericError.ROOM_KICKED
                    )
                } else {
                    roomUser.habboSession.sendHabboResponse(
                        OutgoingR63A.MISC_GENERIC_ERROR,
                        MiscGenericErrorResponse.MiscGenericError.ROOM_KICKED
                    )
                }
            }
            if (notifyClient) {
                if (roomUser.habboSession.release != "R63A") {
                    roomUser.habboSession.sendHabboResponse(Outgoing.ROOM_EXIT)
                } else {
                    roomUser.habboSession.sendHabboResponse(OutgoingR63A.ROOM_EXIT)
                }
            }

            if (roomUser.habboSession.currentRoom == this) {
                roomUser.habboSession.roomUser = null
                roomUser.habboSession.currentRoom = null
                roomUser.habboSession.habboMessenger.notifyFriends()
            }
        }

        roomGamemap.removeRoomUser(roomUser, roomUser.currentVector3.vector2)
        roomUser.stepSeatedVector3?.let {
            roomGamemap.removeRoomUser(roomUser, it.vector2)
        }
        roomUsers.remove(roomUser.virtualID)

        // Notifica o game manager que o usuário saiu
        gameManager.onUserLeaveRoom(roomUser)

        roomTask?.addTask(this, UserPartRoomTask(roomUser))
    }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            params[0] as Boolean
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

            // value is defined by
            //  if((_loc2_ & 1) > 0)
            //         {
            //            _SafeStr_9144 = param1.readString(); - officialRoomPicRef
            //         }
            //         if((_loc2_ & 2) > 0)
            //         {
            //            _SafeStr_8973 = param1.readInteger(); - habboGroupId
            //            _groupName = param1.readString();
            //            _SafeStr_8879 = param1.readString(); - groupBadgeCode
            //         }
            //         if((_loc2_ & 4) > 0)
            //         {
            //            _SafeStr_9315 = param1.readString(); - roomAdName
            //            _SafeStr_9318 = param1.readString(); - roomAdDescription
            //            _SafeStr_8487 = param1.readInteger(); - roomAdExpiresInMin
            //         }
            //         _SafeStr_9049 = (_loc2_ & 8) > 0; - showOwner
            //         _SafeStr_8255 = (_loc2_ & 0x10) > 0; - allowPets
            //         _SafeStr_9542 = (_loc2_ & 0x20) > 0; - displayRoomEntryAd
            var value = if (enterRoom) 32 else 0

            group?.let { value += 2 }

            /*if (showEvents) {
            // todo: events
            //value += 4;
        }*/

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

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(roomData.id)
            writeBoolean(false) // is event
            writeUTF(roomData.name)
            writeUTF(roomData.ownerName)
            writeInt(roomData.state.state)
            writeInt(roomUsers.size)
            writeInt(roomData.usersMax)
            writeUTF(roomData.description)
            writeBoolean(roomData.tradeState == 1)
            writeBoolean(roomData.tradeState == 1)
            writeInt(roomData.score)
            writeInt(roomData.category)
            writeUTF("")

            writeInt(roomData.tags.size)

            roomData.tags.forEach { writeUTF(it) }

            // todo: room icon
            writeInt(1)
            writeInt(0)
            writeInt(0)
            // end room icon

            writeBoolean(roomData.allowPets)
            writeBoolean(roomData.allowPetsEat)
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

        roomItemsToSave.filter { it.furnishing.interactionType.name.startsWith("WIRED_") }
            .filter { it.wiredData != null }.let {
                if (it.isNotEmpty()) ItemDao.saveWireds(it)
            }

        if (roomDimmer != null && roomItemsToSave.any { it == roomDimmer!!.roomItem }) ItemDao.saveDimmer(roomDimmer!!)

        roomItemsToSave.clear()
    }

    fun setFloorItem(
        roomItem: RoomItem,
        position: Vector2,
        rotation: Int,
        roomUser: RoomUser?,
        overrideZ: Double = (-1).toDouble(),
        rollerId: Int = -1,
        rollerDelay: Long = 750
    ): Boolean {
        val newItem = !roomItems.containsKey(roomItem.id)
        roomItem.position.vector2 == position && roomItem.rotation != rotation

        if (position == roomModel.doorVector3.vector2) return false
        if (roomItem.position.vector2 == position && roomItem.rotation == rotation) return false
//        if (roomGamemap.isBlocked(position, true)) return false

        HabboServer.habboGame.itemManager.getAffectedTiles(
            position.x,
            position.y,
            rotation,
            roomItem.furnishing.width,
            roomItem.furnishing.height
        ).forEach {
            if (roomGamemap.isBlocked(it, true) && roomGamemap.cannotStackItem[it.x][it.y]) {
                // Check if the blocked tile is from the same item being moved
                val itemsOnTile = roomGamemap.roomItemMap[it] ?: emptyList()
                val isOwnTile = itemsOnTile.any { item -> item.id == roomItem.id }

                if (!isOwnTile) {
                    // cannot set item, because at least one tile is blocked by another item
                    return false
                }
            }
        }
        val affectedTiles = HashSet<Vector2>()
        val wiredItem: WiredItem? = if (roomItem.furnishing.interactionType.name.startsWith("WIRED_")) {
            if (!newItem) wiredHandler.removeWiredItem(roomItem.position.vector2, roomItem)
            else HabboServer.habboGame.itemManager.getWiredInstance(this, roomItem)
        } else null

        if (!newItem) {
            roomGamemap.removeRoomItem(roomItem)

            HabboServer.habboGame.itemManager.getAffectedTiles(
                roomItem.position.x,
                roomItem.position.y,
                roomItem.rotation,
                roomItem.furnishing.width,
                roomItem.furnishing.height
            ).let {
                it.forEach { vector2 ->
                    roomGamemap.getUsersFromVector2(vector2).forEach { roomUser1 ->
                        roomItem.onUserWalksOff(roomUser1, true)

                        roomUser1.removeUserStatuses()

                        roomUser1.currentVector3 = Vector3(vector2, roomGamemap.getAbsoluteHeight(vector2))
                        roomUser1.updateNeeded = true
                    }
                }

                affectedTiles += it
            }

            roomItem.furnishing.interactor?.onRemove(this, roomUser, roomItem)
        }
        val oldPosition = roomItem.position

        roomItem.position = Vector3(
            position.x,
            position.y,
            if (overrideZ != (-1).toDouble()) overrideZ else roomGamemap.getAbsoluteHeight(position.x, position.y)
        )
        roomItem.rotation = rotation

        roomGamemap.addRoomItem(roomItem)

        roomItem.affectedTiles.let {
            it.forEach { vector2 ->
                roomGamemap.getUsersFromVector2(vector2).forEach { roomUser1 ->
                    roomItem.onUserWalksOn(roomUser1, true)

                    roomUser1.addUserStatuses(roomItem)

                    roomUser1.currentVector3 = Vector3(vector2, roomGamemap.getAbsoluteHeight(vector2))
                    roomUser1.updateNeeded = true
                }
            }

            affectedTiles += it
        }

        if (wiredItem != null) wiredHandler.addWiredItem(position, wiredItem)

        roomItem.furnishing.interactor?.onPlace(this, roomUser, roomItem)

        if (newItem) {
            roomItems[roomItem.id] = roomItem

            roomItem.addToRoom(
                this,
                updateDb = true,
                updateClient = true,
                userName = UserInformationDao.getUserInformationById(roomItem.userId)?.username ?: "No owner name"
            )
        } else {
            // Verifica se houve movimento real de posição
            val hasMoved = oldPosition.vector2 != roomItem.position.vector2

            // Se rollerId for -1, é teleporte/giro instantâneo.
            // Se for >= 0 e houve movimento, é animação (Slide).
            val isAnimation = (rollerId != -1) && hasMoved

            if (!isAnimation) {
                // CASO 1: Teleporte, Giro no lugar ou Colocação manual
                // Envia o pacote Update imediatamente
                roomItem.update(updateDb = true, updateClient = true)
            } else {
                // CASO 2: Animação (Wired ou Roller)

                // 1. Envia o Slide Visual (Tempo 0ms)
                // O cliente começa a mover o item visualmente de Old -> New
                sendHabboResponse(Outgoing.ROOM_ROLLER, oldPosition, roomItem.position, -1, rollerId, roomItem.id)

                // 2. Salva no Banco (Assíncrono para não travar)
                addItemToSave(roomItem)

                // 3. AGENDAMENTO DO UPDATE (A CORREÇÃO DO GLITCH)
                // Espera o tempo da animação para enviar a confirmação da nova rotação/posição
                if (rollerDelay > 0) {
                    HabboServer.applicationScope.launch {
                        delay(rollerDelay)
                        // Envia o pacote Update agora que o item "chegou"
                        // Isso corrige a rotação sem causar o "pulo" visual
                        roomItem.update(updateDb = false, updateClient = true)
                    }
                }
            }
        }

        sendHabboResponse(Outgoing.ROOM_UPDATE_FURNI_STACK, this, affectedTiles)

        roomUsersWithRights.forEach { roomUser1 ->
            if (roomUser1.habboSession?.release != "R63A") {
                roomUser1.habboSession?.sendHabboResponse(
                    Outgoing.FLOOR_PLAN_USED_SQUARES,
                    roomGamemap.roomItemMap.filterValues { roomItems1 -> roomItems1.isNotEmpty() }.keys
                )
            }
        }

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

            roomItem.addToRoom(
                this,
                updateDb = true,
                updateClient = true,
                userName = UserInformationDao.getUserInformationById(roomItem.userId)?.username ?: "No owner name"
            )
        } else {
            roomItem.update(updateDb = true, updateClient = true)
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
                sendHabboResponse(OutgoingR63A.ROOM_FLOOR_ITEM_REMOVE, roomItem)

                HabboServer.habboGame.itemManager.getAffectedTiles(
                    roomItem.position.x,
                    roomItem.position.y,
                    roomItem.rotation,
                    roomItem.furnishing.width,
                    roomItem.furnishing.height
                ).let {
                    it.forEach { vector2 ->
                        roomGamemap.getUsersFromVector2(vector2).forEach { roomUser1 ->
                            roomItem.onUserWalksOff(roomUser1, true)

                            roomUser1.removeUserStatuses()

                            roomUser1.currentVector3 = Vector3(vector2, roomGamemap.getAbsoluteHeight(vector2))
                            roomUser1.updateNeeded = true
                        }
                    }

                    sendHabboResponse(Outgoing.ROOM_UPDATE_FURNI_STACK, this, it)

                    roomUsersWithRights.forEach { roomUser ->
                        if (roomUser.habboSession?.release != "R63A") {
                            roomUser.habboSession?.sendHabboResponse(
                                Outgoing.FLOOR_PLAN_USED_SQUARES,
                                roomGamemap.roomItemMap.filterValues { roomItems1 -> roomItems1.isNotEmpty() }.keys
                            )
                        }
                    }
                }
            }
            ItemType.WALL -> {
                sendHabboResponse(Outgoing.ROOM_WALL_ITEM_REMOVE, roomItem)
                sendHabboResponse(OutgoingR63A.ROOM_WALL_ITEM_REMOVE, roomItem)
            }
            else -> {}
        }

        if (roomItemsToSave.contains(roomItem)) roomItemsToSave.remove(roomItem)

        return true
    }

    fun updateGroupInfo() {
        group?.let { group ->
            roomUsers.values.filter { it.habboSession != null }.forEach {
                it.habboSession?.let { habboSession ->
                    habboSession.sendHabboResponse(
                        Outgoing.GROUP_INFO,
                        habboSession.userInformation.id,
                        habboSession.userStats.favoriteGroupId == group.groupData.id,
                        group,
                        false
                    )
                }
            }
        }
    }

    fun updateGroupRights() {
        group?.let { group ->
            roomUsers.values.filter { it.habboSession != null }
                .filter { it.habboSession?.userInformation?.id != group.groupData.ownerId }.forEach { roomUser ->
                    roomUser.habboSession?.let { habboSession ->
                        val methodName =
                            HabboServer.habboHandler.getOverrideMethodForHeader(
                                Outgoing.ROOM_OWNER,
                                habboSession.release
                            )

                        when {
                            hasRights(habboSession, false) -> {
                                roomUser.addStatus("flatctrl", "1")

                                when (methodName) {
                                    "response" -> habboSession.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, 1)
                                    "responseWithRoomId" -> habboSession.sendHabboResponse(
                                        Outgoing.ROOM_RIGHT_LEVEL,
                                        roomData.id,
                                        1
                                    )
                                    else -> log.error("Couldn't send response!")
                                }
                            }
                            roomUser.statusMap.containsKey("flatctrl") -> {
                                roomUser.removeStatus("flatctrl")

                                when (methodName) {
                                    "response" -> habboSession.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, 0)
                                    "responseWithRoomId" -> habboSession.sendHabboResponse(
                                        Outgoing.ROOM_RIGHT_LEVEL,
                                        roomData.id,
                                        0
                                    )
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
