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

package ovh.rwx.habbo.communication.outgoing.handshake

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class HandshakeAuthenticationOkResponse {
    @Response(Outgoing.AUTHENTICATION_OK)
    @ResponseR63A(OutgoingR63A.HANDSHAKE_AUTHENTICATION_OK)
    fun response(habboResponse: HabboResponse) {
    }

    @Response(Outgoing.AUTHENTICATION_OK)
    fun responseHabboAir(habboResponse: HabboResponse, habboSession: HabboSession) {
        habboResponse.apply {
            writeInt(habboSession.userInformation.id) // accountId
            writeInt(0) // size of suggestedLoginActions
//            writeShort(0) // suggestedLoginActions param 0
//            writeShort(0) // suggestedLoginActions param 1
//            if(isOnboardingRequired(_communicationManager.suggestedLoginActions))
            // isOnboardingRequired => return param1.indexOf(0) >= 0 || param1.indexOf(1) >= 0;
            // OnBoardingHcFlow
            // if(_SafeStr_5729.indexOf(0) >= 0)
            //         {
            //            startNameChange();
            //         }
            //         else
            //         {
            //            startRoomPicking();
            //         }
            // isRoomPickingNeeded - return _SafeStr_5729.indexOf(1) >= 0;
            writeInt(habboSession.userInformation.id) // identityId
        }
    }
}