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

package tk.jomp16.habbo.communication.incoming.handshake

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.encryption.RC4Encryption
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class HandshakeSecretKeyHandler {
    @Handler(Incoming.SECRET_KEY)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (HabboServer.habboConfig.rc4) {
            val sharedKeyPair = HabboServer.habboEncryptionHandler.calculateDiffieHellmanSharedKey(habboSession.diffieHellmanParams, habboRequest.readUTF())

            habboSession.rc4Encryption = RC4Encryption(sharedKeyPair.second)

            habboSession.sendHabboResponse(Outgoing.SECRET_KEY, HabboServer.habboEncryptionHandler.getRsaStringEncrypted(sharedKeyPair.first))
        }
    }
}