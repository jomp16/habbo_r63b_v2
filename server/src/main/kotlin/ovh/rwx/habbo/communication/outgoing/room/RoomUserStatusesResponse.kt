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
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUserStatusesResponse {
    @Response(Outgoing.ROOM_USERS_STATUSES)
    fun response(habboResponse: HabboResponse, roomUsers: Collection<RoomUser>) {
        habboResponse.apply {
            writeInt(roomUsers.size)

            roomUsers.forEach {
                writeInt(it.virtualID)
                writeInt(it.currentVector3.x)
                writeInt(it.currentVector3.y)
                writeUTF(it.currentVector3.z.toString())
                writeInt(it.headRotation)
                writeInt(it.bodyRotation)
                val stringBuilder = StringBuilder("/")

                it.statusMap.entries.filter { it.key.isNotBlank() }.forEach {
                    stringBuilder.append(it.key)

                    if (it.value.second.isNotBlank()) stringBuilder.append(' ').append(it.value.second)

                    stringBuilder.append('/')
                }

                writeUTF(stringBuilder.toString())
            }
        }
    }
}