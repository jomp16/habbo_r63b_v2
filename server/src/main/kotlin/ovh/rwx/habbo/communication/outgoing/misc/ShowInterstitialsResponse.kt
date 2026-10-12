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

package ovh.rwx.habbo.communication.outgoing.misc

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

@Suppress("unused", "UNUSED_PARAMETER")
class ShowInterstitialsResponse {
    @Response(Outgoing.SHOW_INTERSTITIALS)
    @ResponseR63A(OutgoingR63A.SHOW_INTERSTITIALS)
    fun response(habboResponse: HabboResponse) {
        habboResponse.apply {
            if (isVersionBefore(2016, 7, 26)) {
                // Era Flash legada (R35 / R63A até 2015): RoomInterstitialMessageEvent
                writeUTF("") // imageUrl (vazio quando não há anúncio)
                writeUTF("") // clickUrl
                if (isVersionAtLeast(2015, 12, 1)) {
                    writeUTF("") // videoXmlUrl (adicionado no final de 2015)
                }
            } else {
                // Flash / AIR moderno (PRODUCTION-2016 em diante): InterstitialMessageEvent
                writeBoolean(false) // canShowInterstitial
            }
        }
    }
}