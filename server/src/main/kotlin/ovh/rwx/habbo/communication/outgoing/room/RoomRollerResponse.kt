/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.util.Vector3

@Suppress("unused", "UNUSED_PARAMETER")
class RoomRollerResponse {
    @Response(Outgoing.ROOM_ROLLER)
    fun response(habboResponse: HabboResponse, source: Vector3, target: Vector3, virtualId: Int, rollerId: Int, itemId: Int) {
        habboResponse.apply {
            writeInt(source.x)
            writeInt(source.y)
            writeInt(target.x)
            writeInt(target.y)

            if (itemId != -1) {
                writeInt(1) // items count
                writeInt(itemId)
                writeUTF(source.z.toString())
                writeUTF(target.z.toString())
                writeInt(0) // roller ID
            } else {
                writeInt(0) // items count
                writeInt(rollerId)
                writeInt(2) // 1 - move / 2 - slide
                writeInt(virtualId)
                writeUTF(source.z.toString())
                writeUTF(target.z.toString())
                writeInt(rollerId)
            }
        }
    }
}