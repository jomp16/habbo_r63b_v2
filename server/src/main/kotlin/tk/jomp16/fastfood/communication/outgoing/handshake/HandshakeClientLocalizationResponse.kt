/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.fastfood.communication.outgoing.handshake

import tk.jomp16.fastfood.communication.FFResponse
import tk.jomp16.fastfood.communication.outgoing.FFOutgoing
import tk.jomp16.habbo.communication.HabboResponse

@Suppress("unused", "UNUSED_PARAMETER")
class HandshakeClientLocalizationResponse {
    @FFResponse(FFOutgoing.HANDSHAKE_SEND_CLIENT_LOCALIZATIONS)
    fun response(habboResponse: HabboResponse, localizations: Map<String, String>) {
        habboResponse.apply {
            writeInt(localizations.size)

            localizations.forEach { key, value ->
                writeUTF(key)
                writeUTF(value)
            }
        }
    }
}