/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.room

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.game.room.model.SquareState

@Suppress("unused", "UNUSED_PARAMETER")
class RoomFloormapResponse {
    @Response(Outgoing.ROOM_FLOORMAP)
    fun response(habboResponse: HabboResponse, room: Room) {
        habboResponse.apply {
            writeBoolean(true)
            writeInt(room.roomData.wallHeight)
            val stringBuilder = StringBuilder()

            for (i in 0 until room.roomModel.mapSizeY) {
                for (j in 0 until room.roomModel.mapSizeX) {
                    if (j == room.roomModel.doorVector3.x && i == room.roomModel.doorVector3.y) {
                        if (room.roomModel.doorVector3.z > 9) stringBuilder.append(RoomModel.letters.toCharArray()[room.roomModel.doorVector3.z.toInt() - 10])
                        else stringBuilder.append(room.roomModel.doorVector3.z.toInt())
                    } else if (room.roomModel.squareStates[j][i] == SquareState.CLOSED) {
                        stringBuilder.append("x")
                    } else {
                        if (room.roomModel.floorHeight[j][i] > 9) stringBuilder.append(RoomModel.letters.toCharArray()[room.roomModel.floorHeight[j][i] - 10])
                        else stringBuilder.append(room.roomModel.floorHeight[j][i].toInt())
                    }
                }

                stringBuilder.append(13.toChar())
            }

            writeUTF(stringBuilder.toString())
        }
    }
}