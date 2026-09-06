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
import ovh.rwx.habbo.communication.outgoing.room.RoomNoRightsData
import ovh.rwx.habbo.communication.outgoing.room.RoomOpenData
import ovh.rwx.habbo.communication.outgoing.room.RoomOwnerData
import ovh.rwx.habbo.communication.outgoing.room.RoomRightLevelData
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
            room.sendHabboResponse(Outgoing.ROOM_USERS, listOf(roomUser))
            room.sendHabboResponse(
                Outgoing.USER_UPDATE,
                roomUser.virtualID,
                it.userInformation.figure,
                it.userInformation.gender,
                it.userInformation.motto,
                it.userStats.achievementScore
            )
            room.sendHabboResponse(Outgoing.ROOM_USERS_STATUSES, listOf(roomUser))
            room.sendHabboResponse(OutgoingR63A.ROOM_USERS, listOf(roomUser))
            room.sendHabboResponse(OutgoingR63A.ROOM_USERS_STATUSES, listOf(roomUser))
        }

        // 5. Reset do contador de sala vazia
        if (room.emptyCounter.get() > 0) {
            room.emptyCounter.set(0)
            room.roomTimer.set(0)
        }

        // 6. Join concluído no ciclo de vida da sala
        roomUser.pendingJoin = false

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
        if (habboSession.release != "R63A") {
            habboSession.sendHabboResponse(Outgoing.ROOM_OPEN, RoomOpenData(room.roomData.id))
//            habboSession.sendHabboResponse(Outgoing.USER_NFT_CHAT_STYLES, room.roomModel.id, room.roomData.id)
            /*habboSession.sendHabboResponse(HabboResponse(1219, null).apply {
                writeInt(0)
            })*/
            habboSession.sendHabboResponse(Outgoing.ROOM_GROUP_BADGES, room.loadedGroups)
            habboSession.sendHabboResponse(Outgoing.ROOM_INITIAL_INFO, room.roomModel.id, room.roomData.id)
            habboSession.sendHabboResponse(
                Outgoing.FLOOR_PLAN_DOOR,
                room.roomModel.doorVector3,
                room.roomModel.doorDir
            )

            if (room.roomData.wallpaper != "0.0") habboSession.sendHabboResponse(
                Outgoing.ROOM_DECORATION,
                "wallpaper",
                room.roomData.wallpaper
            )
            if (room.roomData.floor != "0.0") habboSession.sendHabboResponse(
                Outgoing.ROOM_DECORATION,
                "floor",
                room.roomData.floor
            )
            if (room.roomData.landscape != "0.0") habboSession.sendHabboResponse(
                Outgoing.ROOM_DECORATION,
                "landscape",
                room.roomData.landscape
            )
            habboSession.sendHabboResponse(Outgoing.ROOM_HEIGHTMAP, room)
            habboSession.sendHabboResponse(Outgoing.ROOM_FLOORMAP, room)
            habboSession.sendHabboResponse(
                Outgoing.ROOM_OWNERSHIP,
                room.roomData.id,
                isOwner
            )
            habboSession.sendHabboResponse(
                Outgoing.ROOM_VISUALIZATION_THICKNESS,
                room.roomData.hideWall,
                room.roomData.wallThick,
                room.roomData.floorThick
            )
            // todo: events

            habboSession.sendHabboResponse(Outgoing.ROOM_USERS, room.userManager.entities.values)
            habboSession.sendHabboResponse(Outgoing.ROOM_USERS_STATUSES, room.userManager.entities.values)

            habboSession.sendHabboResponse(Outgoing.ROOM_FLOOR_ITEMS, room, room.itemManager.floorItems.values)
            habboSession.sendHabboResponse(Outgoing.ROOM_WALL_ITEMS, room, room.itemManager.wallItems.values)

            room.userManager.entities.values.forEach {
                val entityUser = it as? RoomHumanoid
                if (entityUser?.idle == true) habboSession.sendHabboResponse(
                    Outgoing.ROOM_USER_IDLE,
                    it.virtualID,
                    true
                )
                if (entityUser != null && entityUser.danceId > 0) habboSession.sendHabboResponse(
                    Outgoing.ROOM_USER_DANCE,
                    it.virtualID,
                    entityUser.danceId
                )
                if (entityUser != null && entityUser.handItem > 0) habboSession.sendHabboResponse(
                    Outgoing.ROOM_USER_HANDITEM,
                    it.virtualID,
                    entityUser.handItem
                )
                it.effect?.let { effect ->
                    habboSession.sendHabboResponse(
                        Outgoing.ROOM_USER_EFFECT,
                        it.virtualID,
                        effect.effectId
                    )
                }
            }

            habboSession.sendHabboResponse(Outgoing.WIRED_ENVIRONMENT, false)
            habboSession.sendHabboResponse(
                Outgoing.WIRED_PERMISSIONS,
                hasRights,
                hasRights
            )
            // todo: adicionar permissão wired: canModify / canRead

            // Respostas de direitos
            if (hasRights) {
                if (isOwner) {
                    habboSession.sendHabboResponse(Outgoing.ROOM_OWNER, RoomOwnerData(room.roomData.id))
                    habboSession.sendHabboResponse(
                        Outgoing.ROOM_RIGHT_LEVEL,
                        RoomRightLevelData(rightLevel = 4, roomId = room.roomData.id)
                    )
                } else {
                    habboSession.sendHabboResponse(
                        Outgoing.ROOM_RIGHT_LEVEL,
                        RoomRightLevelData(rightLevel = 1, roomId = room.roomData.id)
                    )
                }
            } else {
                habboSession.sendHabboResponse(Outgoing.ROOM_NO_RIGHTS, RoomNoRightsData(room.roomData.id))
            }
        } else {
            habboSession.sendHabboResponse(OutgoingR63A.ROOM_OPEN)
            habboSession.sendHabboResponse(OutgoingR63A.ROOM_URL, "/client/internal/" + room.roomData.id + "/id")
            habboSession.sendHabboResponse(
                OutgoingR63A.ROOM_INITIAL_INFO,
                "model_${room.roomModel.id}",
                room.roomData.id
            )

            if (room.roomData.wallpaper != "0.0") habboSession.sendHabboResponse(
                OutgoingR63A.ROOM_DECORATION,
                "wallpaper",
                room.roomData.wallpaper
            )
            if (room.roomData.floor != "0.0") habboSession.sendHabboResponse(
                OutgoingR63A.ROOM_DECORATION,
                "floor",
                room.roomData.floor
            )
            if (room.roomData.landscape != "0.0") habboSession.sendHabboResponse(
                OutgoingR63A.ROOM_DECORATION,
                "landscape",
                room.roomData.landscape
            )
            habboSession.sendHabboResponse(OutgoingR63A.ROOM_HEIGHTMAP, room)
            habboSession.sendHabboResponse(OutgoingR63A.ROOM_FLOORMAP, room)
            habboSession.sendHabboResponse(
                OutgoingR63A.ROOM_OWNERSHIP,
                room.roomData.roomType == RoomType.PRIVATE,
                room.roomData.id,
                isOwner
            )
            habboSession.sendHabboResponse(
                OutgoingR63A.ROOM_VISUALIZATION_THICKNESS,
                room.roomData.hideWall,
                room.roomData.wallThick,
                room.roomData.floorThick
            )
            // todo: events

            habboSession.sendHabboResponse(OutgoingR63A.ROOM_USERS, room.userManager.entities.values)
            habboSession.sendHabboResponse(OutgoingR63A.ROOM_USERS_STATUSES, room.userManager.entities.values)
            habboSession.sendHabboResponse(OutgoingR63A.ROOM_INFO, habboSession, room, true, false)

            room.userManager.entities.values.forEach {
                val entityUser = it as? RoomHumanoid
                if (entityUser?.idle == true) habboSession.sendHabboResponse(
                    OutgoingR63A.ROOM_USER_IDLE,
                    it.virtualID,
                    true
                )
                if (entityUser != null && entityUser.danceId > 0) habboSession.sendHabboResponse(
                    OutgoingR63A.ROOM_USER_DANCE,
                    it.virtualID,
                    entityUser.danceId
                )
                if (entityUser != null && entityUser.handItem > 0) habboSession.sendHabboResponse(
                    OutgoingR63A.ROOM_USER_HANDITEM,
                    it.virtualID,
                    entityUser.handItem
                )
                it.effect?.let { effect ->
                    habboSession.sendHabboResponse(
                        OutgoingR63A.ROOM_USER_EFFECT,
                        it.virtualID,
                        effect.effectId
                    )
                }
            }

            if (hasRights) {
                if (isOwner) {
                    habboSession.sendHabboResponse(OutgoingR63A.ROOM_OWNER)
                    habboSession.sendHabboResponse(OutgoingR63A.ROOM_RIGHT)
                } else {
                    habboSession.sendHabboResponse(OutgoingR63A.ROOM_RIGHT)
                }
            }
        }
    }
}