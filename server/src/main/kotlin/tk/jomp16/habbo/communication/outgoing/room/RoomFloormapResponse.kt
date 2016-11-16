/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.communication.outgoing.room

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.model.RoomModel
import tk.jomp16.habbo.game.room.model.SquareState

@Suppress("unused", "UNUSED_PARAMETER")
class RoomFloormapResponse {
    @Response(Outgoing.ROOM_FLOORMAP)
    fun response(habboResponse: HabboResponse, room: Room) {
        habboResponse.apply {
            writeBoolean(true)
            writeInt(room.roomData.wallHeight)

            val stringBuilder = StringBuilder()

            for (i in 0..room.roomModel.mapSizeY - 1) {
                for (j in 0..room.roomModel.mapSizeX - 1) {
                    if (j == room.roomModel.doorVector3.x && i == room.roomModel.doorVector3.y) {
                        if (room.roomModel.doorVector3.z > 9) stringBuilder.append(
                                RoomModel.letters.toCharArray()[room.roomModel.doorVector3.z.toInt() - 10])
                        else stringBuilder.append(room.roomModel.doorVector3.z.toInt())
                    } else if (room.roomModel.squareStates[j][i] == SquareState.CLOSED) {
                        stringBuilder.append("x")
                    } else {
                        if (room.roomModel.floorHeight[j][i] > 9) stringBuilder.append(
                                RoomModel.letters.toCharArray()[room.roomModel.floorHeight[j][i] - 10])
                        else stringBuilder.append(room.roomModel.floorHeight[j][i].toInt())
                    }
                }

                stringBuilder.append(13.toChar())
            }

            writeUTF(stringBuilder.toString())
        }
    }
}