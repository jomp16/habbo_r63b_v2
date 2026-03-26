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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.encryption.RC4Encryption
import ovh.rwx.habbo.encryption.WedgieRC4
import ovh.rwx.habbo.encryption.wedgie.HabboRandom
import ovh.rwx.habbo.encryption.wedgie.WedgieWrapper
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class HandshakeSecretKeyHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Handler(Incoming.SECRET_KEY, requiredAuth = false)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (HabboServer.habboConfig.encryptionConfig.rc4) {
            val sharedKeyPair = HabboServer.habboEncryptionHandler.calculateDiffieHellmanSharedKey(
                habboSession.diffieHellmanParams,
                habboRequest.readUTF()
            )

            habboSession.rc4Encryption = RC4Encryption(sharedKeyPair.second.toByteArray())

            habboSession.sendHabboResponse(
                Outgoing.SECRET_KEY,
                HabboServer.habboEncryptionHandler.getRsaStringEncrypted(sharedKeyPair.first.toString().toByteArray())
            )
        }
    }

    @HandlerR63A(IncomingR63A.SECRET_KEY, requiredAuth = false)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        try {
            if (HabboServer.habboConfig.encryptionConfig.rc4) {
                // A partir da build 34379 que tem criptografia RSA
                val publicKey = habboRequest.readUTF()
                val isHex = publicKey.any { it in 'a'..'f' || it in 'A'..'F' }

                val sharedKeyPair = HabboServer.habboEncryptionHandler.calculateDiffieHellmanSharedKey(
                    habboSession.diffieHellmanParams,
                    publicKey,
                    ignoreSign = true,
                    disableRsa = !isHex
                )

                // CORREÇÃO: Simular o comportamento do cliente AS3 (BigInt -> Hex String -> Hex Decode)
                var sharedSecretHex = sharedKeyPair.second.toString(16)
                if (sharedSecretHex.length % 2 != 0) {
                    sharedSecretHex = "0$sharedSecretHex" // Padrona zero à esquerda se for ímpar
                }

                val sharedSecretBytes = Hex.decode(sharedSecretHex)

                habboSession.rc4Encryption = if (isHex) {
                    RC4Encryption(sharedSecretBytes)
                } else {
                    // O AS3 pega os últimos 4 dígitos dessa string Base 10 e converte para int usando base 16
                    val seedStr = habboSession.cryptoToken.takeLast(4)
                    val seed = seedStr.toInt(16)

                    WedgieWrapper(
                        inner = WedgieRC4(sharedSecretBytes),
                        outer = WedgieRC4(sharedSecretBytes),
                        rng = HabboRandom(seed)
                    )
                }

                // Enviamos nossa chave pública pro cliente
                // O cliente espera a chave pública do servidor em TEXTO PLANO (Base 10).
                val serverPublicKeyStr = sharedKeyPair.first.toString(10)
                habboSession.sendHabboResponse(OutgoingR63A.SECRET_KEY, serverPublicKeyStr)
            }
        } catch (e: Exception) {
            log.error("Handshake secret key handler error", e)
            habboSession.sendHabboResponse(OutgoingR63A.HANDSHAKE_SESSION_PARAMS)
        }
    }
}