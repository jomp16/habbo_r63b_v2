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

package ovh.rwx.habbo.encryption

import org.bouncycastle.util.encoders.Hex
import ovh.rwx.habbo.HabboServer
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import javax.crypto.KeyAgreement
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.DHPublicKeySpec

class HabboEncryptionHandler(n: String, d: String, e: String) {
    private val rsaEncryption: RSAEncryption = RSAEncryption(n, d, e)
    private val diffieHellmanEncryption: DiffieHellmanEncryption = DiffieHellmanEncryption()
    private var dhParameterSpec: DHParameterSpec? = null
    private var serverKeyPair: KeyPair? = null

    init {
        if (!HabboServer.habboConfig.encryptionConfig.diffieHellmanConfig.alwaysGenerateNewKeys) {
            dhParameterSpec =
                diffieHellmanEncryption.getDHParameterSpec(HabboServer.habboConfig.encryptionConfig.diffieHellmanConfig.keySize)

            serverKeyPair = KeyPairGenerator.getInstance("DH", "BC").run {
                initialize(dhParameterSpec)

                genKeyPair()
            }
        }
    }

    fun generateDiffieHellmanParameterSpec(): DHParameterSpec = when {
        HabboServer.habboConfig.encryptionConfig.diffieHellmanConfig.alwaysGenerateNewKeys -> diffieHellmanEncryption.getDHParameterSpec(
            HabboServer.habboConfig.encryptionConfig.diffieHellmanConfig.keySize
        )

        else -> dhParameterSpec!!
    }

    fun calculateDiffieHellmanSharedKey(
        diffieHellmanParams: DHParameterSpec,
        publicKey: String,
        ignoreSign: Boolean = false,
        disableRsa: Boolean = false,
    ): Pair<BigInteger, BigInteger> {
        val clientPublicKeyValue = if (disableRsa) {
            BigInteger(publicKey)
        } else {
            val verifiedBytes = rsaEncryption.verify(Hex.decode(publicKey))
            if (verifiedBytes.isEmpty()) {
                throw IllegalArgumentException("Invalid RSA payload from client")
            }
            BigInteger(verifiedBytes.toString(Charsets.UTF_8))
        }

        val clientPublicKey = KeyFactory.getInstance("DH", "BC").run {
            generatePublic(
                DHPublicKeySpec(
                    clientPublicKeyValue,
                    diffieHellmanParams.p,
                    diffieHellmanParams.g
                )
            )
        }

        val serverKeyPair1: KeyPair =
            if (HabboServer.habboConfig.encryptionConfig.diffieHellmanConfig.alwaysGenerateNewKeys) {
                KeyPairGenerator.getInstance("DH", "BC").run {
                    initialize(diffieHellmanParams)

                    genKeyPair()
                }
            } else {
                serverKeyPair!!
            }

        // KeyAgreement is stateful and NOT thread-safe; instantiate a fresh one
        // per handshake. KeyPair / DHParameterSpec are immutable and safe to cache.
        val localKeyAgree = KeyAgreement.getInstance("DH", "BC").apply {
            init(serverKeyPair1.private)
        }

        localKeyAgree.doPhase(clientPublicKey, true)

        return if (ignoreSign) {
            (serverKeyPair1.public as DHPublicKey).y to BigInteger(1, localKeyAgree.generateSecret())
        } else {
            (serverKeyPair1.public as DHPublicKey).y to BigInteger(localKeyAgree.generateSecret())
        }
    }

    fun getRsaStringEncrypted(bytes: ByteArray): String = Hex.toHexString(rsaEncryption.sign(bytes))
}
