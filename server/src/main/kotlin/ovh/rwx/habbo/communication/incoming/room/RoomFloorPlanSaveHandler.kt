/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.incoming.room

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.misc.MiscSuperNotificationResponse
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.game.room.gamemap.RoomGamemap
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.Vector3

@Suppress("unused", "UNUSED_PARAMETER")
class RoomFloorPlanSaveHandler {
    private val validLetters = listOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
            'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', '\r', '\n'
    )

    @Handler(Incoming.FLOOR_PLAN_SAVE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null || !habboSession.currentRoom!!.hasRights(habboSession, true)) return

        habboSession.currentRoom!!.let { room ->
            val heightmap = habboRequest.readUTF()
            val doorX = habboRequest.readInt()
            val doorY = habboRequest.readInt()
            val doorDir = habboRequest.readInt()
            val wallThickness = habboRequest.readInt()
            val floorThickness = habboRequest.readInt()
            val wallHeight = habboRequest.readInt()

            if (heightmap.isEmpty() || heightmap.length < 2) return
            if (wallThickness < -2 || wallThickness > 1) {
                habboSession.sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.FLOOR_PLAN_EDITOR_ERROR, "errors", "(%%%general%%%): %%%invalid_wall_thickness%%%")
                return
            }
            if (floorThickness < -2 || floorThickness > 1) {
                habboSession.sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.FLOOR_PLAN_EDITOR_ERROR, "errors", "(%%%general%%%): %%%invalid_floor_thickness%%%")
                return
            }
            if (wallHeight < -1 || wallHeight > 15) {
                habboSession.sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.FLOOR_PLAN_EDITOR_ERROR, "errors", "(%%%general%%%): %%%invalid_walls_fixed_height%%%")
                return
            }

            heightmap.toCharArray().forEach { c ->
                if (!validLetters.contains(c)) {
                    habboSession.sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.FLOOR_PLAN_EDITOR_ERROR)

                    return
                }
            }

            // Todo: make it secure by adding more validations

            val roomModel = RoomModel("0", room.roomData.id, Vector3(doorX, doorY, (-1).toDouble()), doorDir, heightmap.trim().split("[\\r\\n]+".toRegex()), false)

            if (roomModel.mapSizeX <= doorX || roomModel.mapSizeY <= doorY) {
                habboSession.sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.FLOOR_PLAN_EDITOR_ERROR, "errors", "(%%%general%%%): %%%entry_tile_outside_map%%%")
                return
            }

            if (room.roomData.modelName == "custom") {
                // Update custom room model
                roomModel.id = room.roomModel.id

                RoomDao.updateCustomRoomModel(roomModel)
            } else {
                // Insert new room model
                room.roomData.modelName = "custom"

                RoomDao.insertCustomRoomModel(roomModel)

                HabboServer.habboGame.roomManager.customRoomModels[roomModel.roomId] = roomModel
            }

            room.roomModel = roomModel
            room.roomData.wallHeight = wallHeight
            room.roomData.floorThick = floorThickness
            room.roomData.wallThick = wallThickness

            val roomUsers = room.roomUsers.values.toList()

            HabboServer.habboGame.roomManager.roomTaskManager.removeRoomFromTask(room)

            room.roomGamemap = RoomGamemap(room)

            roomUsers.map { GlobalScope.async { it.habboSession?.sendHabboResponse(Outgoing.ROOM_FORWARD, room.roomData.id) } }
        }
    }
}