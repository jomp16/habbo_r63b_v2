/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import java.time.LocalDateTime

@Suppress("unused", "UNUSED_PARAMETER")
class UserObjectResponse {
    @Response(Outgoing.USER_OBJECT)
    fun response(habboResponse: HabboResponse, id: Int, username: String, figure: String, gender: String, motto: String, respect: Int, dailyRespectPoints: Int, dailyPetRespectPoints: Int, lastOnline: LocalDateTime, canChangeName: Boolean) {
        habboResponse.apply {
            writeInt(id)
            writeUTF(username)
            writeUTF(figure)
            writeUTF(gender)
            writeUTF(motto)
            writeUTF("")
            writeBoolean(false)
            writeInt(respect)
            writeInt(dailyRespectPoints)
            writeInt(dailyPetRespectPoints)
            writeBoolean(true) // Friends stream active
            writeUTF(lastOnline.format(HabboServer.DATE_TIME_FORMATTER_WITH_HOURS))
            writeBoolean(canChangeName)
            writeBoolean(false)
        }
    }
}