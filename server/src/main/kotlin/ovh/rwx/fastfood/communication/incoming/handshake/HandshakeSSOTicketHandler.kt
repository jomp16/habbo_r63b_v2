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

package ovh.rwx.fastfood.communication.incoming.handshake

import ovh.rwx.fastfood.communication.FFHandler
import ovh.rwx.fastfood.communication.incoming.FFIncoming
import ovh.rwx.fastfood.communication.outgoing.FFOutgoing
import ovh.rwx.fastfood.game.FastFoodSession
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest

@Suppress("unused", "UNUSED_PARAMETER")
class HandshakeSSOTicketHandler {
    @FFHandler(FFIncoming.HANDSHAKE_SSO_TICKET)
    fun handle(fastFoodSession: FastFoodSession, habboRequest: HabboRequest) {
        val ssoTicket = habboRequest.readUTF()
        @Suppress("UNUSED_VARIABLE")
        val hotelHost = habboRequest.readUTF()
        // todo: authenticate with sso ticket please
        val habboSession = HabboServer.habboSessionManager.habboSessions.values.filter { it.gameSSOToken.isNotBlank() }.firstOrNull { it.gameSSOToken == ssoTicket }

        if (habboSession != null) {
            habboSession.gameSSOToken = ""
            fastFoodSession.habboSession = habboSession

            fastFoodSession.sendHabboResponse(FFOutgoing.HANDSHAKE_AUTHENTICATION_OK)
        } else {
            // disconnect user pls
            fastFoodSession.channel.disconnect()
        }
    }
}