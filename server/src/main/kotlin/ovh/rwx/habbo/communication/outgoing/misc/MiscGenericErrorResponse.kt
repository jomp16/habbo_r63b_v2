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

package ovh.rwx.habbo.communication.outgoing.misc

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class MiscGenericErrorResponse {
    @Response(Outgoing.MISC_GENERIC_ERROR)
    fun response(habboResponse: HabboResponse, miscGenericError: MiscGenericError) {
        habboResponse.apply {
            writeInt(miscGenericError.errorCode)
        }
    }

    enum class MiscGenericError(val errorCode: Int) {
        WRONG_PASSWORD(-100002), // did_not_receive_code_link = true and retry_wait_label = false
        ROOM_KICKED(4008), // room.error.kicked
        NEED_TO_BE_VIP(4009), // navigator.alert.need.to.be.vip
        INVALID_ROOM_NAME(4010), // navigator.alert.invalid_room_name
        CANNOT_PERM_BAN(4011), // navigator.alert.cannot_perm_ban
        ROOM_IN_MAINTENANCE(4013) // navigator.alert.room_in_maintenance
    }
}