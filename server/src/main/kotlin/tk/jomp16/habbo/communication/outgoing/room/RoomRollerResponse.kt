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
import tk.jomp16.habbo.util.Vector3

@Suppress("unused", "UNUSED_PARAMETER")
class RoomRollerResponse {
    @Response(Outgoing.ROOM_ROLLER)
    fun handle(habboResponse: HabboResponse, source: Vector3, target: Vector3, virtualId: Int, rollerId: Int, itemId: Int) {
        habboResponse.apply {
            writeInt(source.x)
            writeInt(source.y)
            writeInt(target.x)
            writeInt(target.y)
            writeInt(if (itemId != -1) 1 else 0)

            if (itemId != -1) {
                writeInt(itemId)
            } else {
                writeInt(rollerId)
                writeInt(2)
                writeInt(virtualId)
            }

            writeUTF(source.z.toString())
            writeUTF(target.z.toString())

            if (itemId != -1) writeInt(rollerId)
        }
    }
}