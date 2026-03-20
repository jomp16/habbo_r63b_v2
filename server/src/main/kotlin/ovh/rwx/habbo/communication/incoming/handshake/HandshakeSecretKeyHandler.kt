/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.incoming.handshake

import org.bouncycastle.util.encoders.Hex
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.encryption.RC4Encryption
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class HandshakeSecretKeyHandler {
    @Handler(Incoming.SECRET_KEY, requiredAuth = false)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (HabboServer.habboConfig.encryptionConfig.rc4) {
            val sharedKeyPair = HabboServer.habboEncryptionHandler.calculateDiffieHellmanSharedKey(habboSession.diffieHellmanParams, habboRequest.readUTF())

            habboSession.rc4Encryption = RC4Encryption(sharedKeyPair.second.toByteArray())

            habboSession.sendHabboResponse(Outgoing.SECRET_KEY, HabboServer.habboEncryptionHandler.getRsaStringEncrypted(sharedKeyPair.first.toString().toByteArray()))
        }
    }

    @HandlerR63A(IncomingR63A.SECRET_KEY, requiredAuth = false)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        try {
            if (HabboServer.habboConfig.encryptionConfig.rc4) {
                val sharedKeyPair = HabboServer.habboEncryptionHandler.calculateDiffieHellmanSharedKey(
                    habboSession.diffieHellmanParams,
                    habboRequest.readUTF()
                )

                // CORREÇÃO: Simular o comportamento do cliente AS3 (BigInt -> Hex String -> Hex Decode)
                var sharedSecretHex = sharedKeyPair.second.toString(16)
                if (sharedSecretHex.length % 2 != 0) {
                    sharedSecretHex = "0$sharedSecretHex" // Padrona zero à esquerda se for ímpar
                }

                habboSession.rc4Encryption = RC4Encryption(Hex.decode(sharedSecretHex))

                // Enviamos nossa chave pública pro cliente
                // O cliente espera a chave pública do servidor em TEXTO PLANO (Base 10).
                val serverPublicKeyStr = sharedKeyPair.first.toString(10)
                habboSession.sendHabboResponse(OutgoingR63A.SECRET_KEY, serverPublicKeyStr)
            }
        } catch (e: Exception) {
            habboSession.sendHabboResponse(OutgoingR63A.HANDSHAKE_SESSION_PARAMS)
        }
    }
}