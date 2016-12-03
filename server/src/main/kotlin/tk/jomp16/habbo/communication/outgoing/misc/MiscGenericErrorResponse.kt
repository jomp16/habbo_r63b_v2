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

package tk.jomp16.habbo.communication.outgoing.misc

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class MiscGenericErrorResponse {
    @Response(Outgoing.GENERIC_ERROR)
    fun response(habboResponse: HabboResponse, errorCode: Int) {
        habboResponse.apply {
            // -100002 == did_not_receive_code_link = true and retry_wait_label = false
            // 4009 == navigator.alert.need.to.be.vip
            // 4010 == navigator.alert.invalid_room_name
            // 4011 == navigator.alert.cannot_perm_ban
            // 4013 == navigator.alert.room_in_maintenance

            writeInt(errorCode)
        }
    }
}