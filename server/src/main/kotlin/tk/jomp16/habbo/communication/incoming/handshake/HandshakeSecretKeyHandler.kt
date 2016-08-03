/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.communication.incoming.handshake

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.encryption.RC4Encryption
import tk.jomp16.habbo.game.user.HabboSession
import java.math.BigInteger

@Suppress("unused", "UNUSED_PARAMETER")
class HandshakeSecretKeyHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Handler(Incoming.SECRET_KEY)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (HabboServer.habboConfig.rc4) {
            val sharedKey = HabboServer.habboEncryptionHandler.calculateDiffieHellmanSharedKey(habboRequest.readUTF())

            if (sharedKey == BigInteger.ZERO) {
                log.error("Couldn't generate shared key. Probably a RSA error. Disconnecting user!")

                habboSession.channel.close()

                return
            }

            habboSession.rc4Encryption = RC4Encryption(sharedKey.toByteArray())
        }

        habboSession.sendHabboResponse(Outgoing.SECRET_KEY,
                                       HabboServer.habboEncryptionHandler.rsaDiffieHellmanPublicKey)
    }
}