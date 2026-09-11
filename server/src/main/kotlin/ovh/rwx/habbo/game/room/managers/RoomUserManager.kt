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

import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.misc.MiscGenericErrorResponse
import ovh.rwx.habbo.communication.outgoing.room.RoomRightLevelData
import ovh.rwx.habbo.database.pet.PetDao
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.game.pet.PetData
import ovh.rwx.habbo.game.room.RightData
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.UserJoinRoomTask
import ovh.rwx.habbo.game.room.tasks.UserPartRoomTask
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomPet
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.Vector3
import java.util.concurrent.ConcurrentHashMap

class RoomUserManager(private val room: Room) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val rights: MutableSet<RightData> by lazy { HashSet(RoomDao.getRights(room.roomData.id)) }
    val entities: MutableMap<Int, RoomEntity> by lazy { ConcurrentHashMap() }
    val usersWithRights: Set<RoomEntity>
        get() = entities.values.filterIsInstance<RoomUser>().filter { hasRights(it) }.toSet()

    fun hasRights(
        habboSession: HabboSession?,
        ownerRight: Boolean = false,
        ignorePermissionAnyRoomOwner: Boolean = false
    ): Boolean {
        if (habboSession == null) return false

        val userId = habboSession.userInformation.id
        val isOwner =
            room.roomData.ownerId == userId || (!ignorePermissionAnyRoomOwner && habboSession.hasPermission("acc_any_room_owner"))

        if (ownerRight) return isOwner
        if (isOwner) return true

        room.group?.let { g ->
            val isGroupAdmin = g.admins.any { it.userId == userId } || habboSession.hasPermission("acc_any_group_admin")
            if (g.groupData.onlyAdminCanDecorateRoom) {
                if (isGroupAdmin) return true
            } else {
                if (g.members.any { it.userId == userId } || isGroupAdmin) return true
            }
        }

        return rights.any { it.userId == userId }
    }

    fun hasRights(
        roomEntity: RoomEntity?,
        ownerRight: Boolean = false,
        ignorePermissionAnyRoomOwner: Boolean = false
    ): Boolean {
        if (roomEntity == null) return false
        val roomUser = roomEntity as? RoomUser ?: return false
        return hasRights(roomUser.habboSession, ownerRight, ignorePermissionAnyRoomOwner)
    }

    fun addUser(habboSession: HabboSession) {
        if (!room.running || entities.values.filterIsInstance<RoomUser>()
                .any { it.habboSession == habboSession }
        ) return

        var virtualId: Int

        do {
            virtualId = (1..Int.MAX_VALUE).random()
        } while (entities.containsKey(virtualId))

        val newUser =
            RoomUser(
                habboSession,
                room,
                virtualId,
                room.roomModel.doorVector3,
                room.roomModel.doorDir,
                room.roomModel.doorDir
            )

        // Adiciona o usuário imediatamente ao mapa para permitir remoção em caso de disconnect
        // O estado pendingJoin=true indica que o join ainda não foi completado
        entities[virtualId] = newUser

        // Enfileira a tarefa de join que vai completar a inicialização e definir pendingJoin=false
        room.addTask(UserJoinRoomTask(newUser))
    }

    fun loadPets() {
        PetDao.getPetsByRoomId(room.roomData.id).forEach { addPet(it) }
    }

    fun addPet(petData: PetData) {
        var virtualId: Int

        do {
            virtualId = (1..Int.MAX_VALUE).random()
        } while (entities.containsKey(virtualId))

        val roomPet = RoomPet(
            petData,
            room,
            virtualId,
            Vector3(petData.x, petData.y, petData.z),
            petData.rot,
            petData.rot
        )

        roomPet.pendingJoin = false

        entities[virtualId] = roomPet
        room.roomGamemap.addRoomEntity(roomPet, roomPet.currentVector3.vector2)

        // Broadcast to all users
        room.sendResponse(Outgoing.ROOM_USERS, OutgoingR63A.ROOM_USERS, listOf(roomPet))
        room.sendResponse(Outgoing.ROOM_USERS_STATUSES, OutgoingR63A.ROOM_USERS_STATUSES, listOf(roomPet))
    }

    fun removeEntity(roomEntity: RoomEntity?, notifyClient: Boolean, kickNotification: Boolean) {
        if (roomEntity == null) return

        val existed = entities.remove(roomEntity.virtualID) != null
        if (!existed && !roomEntity.pendingJoin) return

        if (roomEntity is RoomUser) {
            // Cancela qualquer troca ativa do usuário antes de remover
            room.tradeManager.onUserDisconnect(roomEntity)
            val session = roomEntity.habboSession
            // Baús com auto-lock: trancam quando o dono sai do quarto
            HabboServer.habboGame.chestManager.onUserLeaveRoom(room, session)
            handleUserDisconnectionMessages(session, notifyClient, kickNotification)
            if (session.currentRoom == room) {
                session.roomUser = null
                session.currentRoom = null
                HabboServer.applicationScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    session.habboMessenger.notifyFriends()
                }
            }
        }

        // Remove do gamemap (se o usuário já foi adicionado)
        room.roomGamemap.removeRoomEntity(roomEntity, roomEntity.currentVector3.vector2)
        roomEntity.nextStepVector?.let { room.roomGamemap.removeRoomEntity(roomEntity, it.vector2) }

        entities.remove(roomEntity.virtualID)

        // Só envia UserPartRoomTask se o usuário já tinha completado o join
        // Usuários em pendingJoin nunca foram oficialmente adicionados ao room tick
        if (!roomEntity.pendingJoin) {
            room.gameManager.onEntityLeaveRoom(roomEntity)
            room.addTask(UserPartRoomTask(roomEntity))
        }
    }

    private fun handleUserDisconnectionMessages(
        session: HabboSession,
        notifyClient: Boolean,
        kickNotification: Boolean
    ) {
        if (kickNotification) {
            session.sendResponse(
                Outgoing.MISC_GENERIC_ERROR,
                OutgoingR63A.MISC_GENERIC_ERROR,
                MiscGenericErrorResponse.MiscGenericError.ROOM_KICKED
            )
        }

        if (notifyClient) {
            session.sendResponse(Outgoing.ROOM_EXIT, OutgoingR63A.ROOM_EXIT)
        }
    }

    fun updateGroupInfo() {
        room.group?.let { g ->
            entities.values.filterIsInstance<RoomUser>().map { it.habboSession }.forEach { session ->
                session.sendHabboResponse(
                    Outgoing.GROUP_INFO,
                    session.userInformation.id,
                    session.userStats.favoriteGroupId == g.groupData.id,
                    g,
                    false
                )
            }
        }
    }

    fun updateGroupRights() {
        val currentGroup = room.group ?: return

        entities.values.filterIsInstance<RoomUser>()
            .filter { it.habboSession.userInformation.id != currentGroup.groupData.ownerId }
            .forEach { roomUser ->
                val session = roomUser.habboSession
                val hasPermission = hasRights(session, false)
                val statusKey = "flatctrl"

                if (hasPermission) {
                    roomUser.addStatus(statusKey, "1")
                    dispatchRightsResponse(session, 1)
                } else if (roomUser.statusMap.containsKey(statusKey)) {
                    roomUser.removeStatus(statusKey)
                    dispatchRightsResponse(session, 0)
                }
            }
    }

    private fun dispatchRightsResponse(session: HabboSession, level: Int) {
        session.sendResponse(
            Outgoing.ROOM_RIGHT_LEVEL,
            if (level > 0) OutgoingR63A.ROOM_RIGHT else null,
            RoomRightLevelData(rightLevel = level, roomId = room.roomData.id)
        )
    }
}
