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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.room.*
import ovh.rwx.habbo.game.item.wired.trigger.RoomEventTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerEnterRoom
import ovh.rwx.habbo.game.room.IRoomTask
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomType
import ovh.rwx.habbo.game.room.user.RoomHumanoid
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession

class UserJoinRoomTask(private val roomUser: RoomUser) : IRoomTask {
    override fun executeTask(room: Room) {
        val session = roomUser.habboSession
        if (!session.channel.isActive || !session.channel.isOpen) {
            // Socket já encerrou: cancela a entrada e assegura a limpeza total
            room.userManager.removeEntity(roomUser, notifyClient = false, kickNotification = false)
            return
        }

        // Verifica se o usuário ainda consta no registro de entidades da sala
        if (!room.userManager.entities.containsKey(roomUser.virtualID)) return

        // Se pendingJoin for false, o join já foi processado anteriormente
        if (!roomUser.pendingJoin) return

        session.roomUser = roomUser

        // 1. Tratamento de teletransporte síncrono no grid
        if (session.teleporting) {
            room.itemManager.items[session.targetTeleportId]?.let { teleportItem ->
                if (!teleportItem.interactingUsers.containsKey(2)) {
                    roomUser.walkingBlocked = true
                    roomUser.currentVector3 = teleportItem.position
                    roomUser.headRotation = teleportItem.rotation
                    roomUser.bodyRotation = teleportItem.rotation

                    teleportItem.interactingUsers[2] = roomUser
                    teleportItem.extraData = "2"
                    teleportItem.update(updateDb = false, updateClient = true)
                    teleportItem.requestTicks(2)
                }
                session.targetTeleportId = 0
            }
        }

        // 2. Conexão de status e verificação de direitos na thread da sala
        val hasRights = room.userManager.hasRights(session)
        val isOwner = room.userManager.hasRights(session, true)
        if (hasRights) {
            roomUser.addStatus("flatctrl", if (isOwner) "4" else "1")
        }

        // 3. Registra a posição física da entidade no mapa de colisão (Gamemap)
        room.roomGamemap.addRoomEntity(roomUser, roomUser.currentVector3.vector2)

        // 4. Broadcast de entrada leve para os usuários presentes na sala
        session.let {
            room.sendResponse(Outgoing.ROOM_USERS, OutgoingR63A.ROOM_USERS, listOf(roomUser))
            /*room.sendHabboResponse(
                Outgoing.USER_UPDATE,
                RoomUpdateUserData(
                    virtualId = roomUser.virtualID,
                    figure = it.userInformation.figure,
                    gender = it.userInformation.gender,
                    motto = it.userInformation.motto,
                    achievementScore = it.userStats.achievementScore
                )
            )*/
            room.sendResponse(Outgoing.ROOM_USERS_STATUSES, OutgoingR63A.ROOM_USERS_STATUSES, listOf(roomUser))
        }

        // 5. Reset do contador de sala vazia
        if (room.emptyCounter.get() > 0) {
            room.emptyCounter.set(0)
            room.roomTimer.set(0)
        }

        // 6. Join concluído no ciclo de vida da sala
        roomUser.pendingJoin = false
        room.wiredVariableManager.onUserEnter(session.userInformation.id)

        // 7. Disparo dos Wireds locais
        room.itemManager.wiredHandler.triggerWired(WiredTriggerEnterRoom::class, roomUser, RoomEventTriggerData)

        // 8. Despacho assíncrono de dados pesados e sistemas externos fora da thread do quarto
        val ownerId = room.roomData.ownerId
        val userId = session.userInformation.id

        HabboServer.applicationScope.launch(Dispatchers.Default) {
            if (!session.channel.isActive) return@launch

            // Envia todos os pacotes estruturais do quarto (mapas, mobis, entidades)
            sendInitialRoomData(session, room, isOwner, hasRights)

            // Sistemas com I/O de banco / mensageria global rodam em pool dedicado
            launch(Dispatchers.IO) {
                session.habboMessenger.notifyFriends()

                if (ownerId != userId) {
                    HabboServer.habboGame.achievementManager.progress(session, "ACH_RoomEntry", 1, accumulate = true)
                }
            }
        }
    }

    /**
     * Serializa e despacha a carga inicial pesada do quarto para a sessão de forma assíncrona.
     */
    private fun sendInitialRoomData(
        habboSession: HabboSession,
        room: Room,
        isOwner: Boolean,
        hasRights: Boolean
    ) {
        habboSession.sendResponse(Outgoing.ROOM_OPEN, OutgoingR63A.ROOM_OPEN, RoomOpenData(room.roomData.id))
        habboSession.sendResponse(
            null,
            OutgoingR63A.ROOM_URL,
            RoomUrlData("/client/internal/" + room.roomData.id + "/id")
        )
        habboSession.sendResponse(Outgoing.ROOM_GROUP_BADGES, OutgoingR63A.ROOM_GROUPS_BADGES, room.loadedGroups)
        habboSession.sendResponse(
            Outgoing.ROOM_INITIAL_INFO,
            OutgoingR63A.ROOM_INITIAL_INFO,
            RoomInitialInfoData(room.roomModel.id, room.roomData.id)
        )
        habboSession.sendResponse(
            Outgoing.FLOOR_PLAN_DOOR,
            null,
            RoomFloorPlanDoorData(room.roomModel.doorVector3, room.roomModel.doorDir)
        )

        if (room.roomData.wallpaper != "0.0") {
            habboSession.sendResponse(
                Outgoing.ROOM_DECORATION,
                OutgoingR63A.ROOM_DECORATION,
                RoomDecorationData(
                    type = "wallpaper",
                    value = room.roomData.wallpaper
                )
            )
        }
        if (room.roomData.floor != "0.0") {
            habboSession.sendResponse(
                Outgoing.ROOM_DECORATION,
                OutgoingR63A.ROOM_DECORATION,
                RoomDecorationData(
                    type = "floor",
                    value = room.roomData.floor
                )
            )
        }
        if (room.roomData.landscape != "0.0") {
            habboSession.sendResponse(
                Outgoing.ROOM_DECORATION,
                OutgoingR63A.ROOM_DECORATION,
                RoomDecorationData(
                    type = "landscape",
                    value = room.roomData.landscape
                )
            )
        }

        habboSession.sendResponse(Outgoing.ROOM_HEIGHTMAP, OutgoingR63A.ROOM_HEIGHTMAP, room)
        habboSession.sendResponse(Outgoing.ROOM_FLOORMAP, OutgoingR63A.ROOM_FLOORMAP, room)
        habboSession.sendResponse(
            Outgoing.ROOM_OWNERSHIP,
            OutgoingR63A.ROOM_OWNERSHIP,
            RoomOwnershipData(
                roomId = room.roomData.id,
                isOwner = isOwner,
                isPrivate = room.roomData.roomType == RoomType.PRIVATE
            )
        )
        habboSession.sendResponse(
            Outgoing.ROOM_VISUALIZATION_THICKNESS,
            OutgoingR63A.ROOM_VISUALIZATION_THICKNESS,
            RoomVisualizationThicknessData(
                hideWall = room.roomData.hideWall,
                wallThickness = room.roomData.wallThick,
                floorThickness = room.roomData.floorThick
            )
        )
        // todo: events

        habboSession.sendResponse(Outgoing.ROOM_USERS, OutgoingR63A.ROOM_USERS, room.userManager.entities.values)
        habboSession.sendResponse(
            Outgoing.ROOM_USERS_STATUSES,
            OutgoingR63A.ROOM_USERS_STATUSES,
            room.userManager.entities.values
        )
        habboSession.sendResponse(
            null,
            OutgoingR63A.ROOM_INFO,
            RoomInfoData(habboSession, room, isLoading = true, checkEntry = false)
        )

        habboSession.sendResponse(
            Outgoing.ROOM_FLOOR_ITEMS,
            null,
            RoomFloorItemsData(room, room.itemManager.floorItems.values)
        )
        habboSession.sendResponse(
            Outgoing.ROOM_WALL_ITEMS,
            null,
            RoomWallItemsData(room, room.itemManager.wallItems.values)
        )

        room.userManager.entities.values.forEach {
            val entityUser = it as? RoomHumanoid
            if (entityUser?.idle == true) {
                habboSession.sendResponse(
                    Outgoing.ROOM_USER_IDLE,
                    OutgoingR63A.ROOM_USER_IDLE,
                    it.virtualID,
                    true
                )
            }
            if (entityUser != null && entityUser.danceId > 0) {
                habboSession.sendResponse(
                    Outgoing.ROOM_USER_DANCE,
                    OutgoingR63A.ROOM_USER_DANCE,
                    it.virtualID,
                    entityUser.danceId
                )
            }
            if (entityUser != null && entityUser.handItem > 0) {
                habboSession.sendResponse(
                    Outgoing.ROOM_USER_HANDITEM,
                    OutgoingR63A.ROOM_USER_HANDITEM,
                    it.virtualID,
                    entityUser.handItem
                )
            }
            it.effect?.let { effect ->
                habboSession.sendResponse(
                    Outgoing.ROOM_USER_EFFECT,
                    OutgoingR63A.ROOM_USER_EFFECT,
                    it.virtualID,
                    effect.effectId
                )
            }
        }

        habboSession.sendResponse(Outgoing.WIRED_ENVIRONMENT, null, false)
        habboSession.sendResponse(
            Outgoing.WIRED_PERMISSIONS,
            null,
            hasRights,
            hasRights
        )
        // todo: adicionar permissão wired: canModify / canRead

        // Respostas de direitos
        if (hasRights) {
            if (isOwner) {
                habboSession.sendResponse(Outgoing.ROOM_OWNER, OutgoingR63A.ROOM_OWNER, RoomOwnerData(room.roomData.id))
            }
            habboSession.sendResponse(
                Outgoing.ROOM_RIGHT_LEVEL,
                OutgoingR63A.ROOM_RIGHT,
                RoomRightLevelData(rightLevel = if (isOwner) 4 else 1, roomId = room.roomData.id)
            )
        } else {
            habboSession.sendResponse(
                Outgoing.ROOM_NO_RIGHTS,
                OutgoingR63A.ROOM_NO_RIGHTS,
                RoomNoRightsData(room.roomData.id)
            )
        }
    }
}