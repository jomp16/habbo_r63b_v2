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

package tk.jomp16.habbo.encryption

import org.bouncycastle.util.encoders.Hex
import tk.jomp16.habbo.HabboServer
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
    private var serverKeyAgree: KeyAgreement? = null

    init {
        if (!HabboServer.habboConfig.encryptionConfig.diffieHellmanConfig.alwaysGenerateNewKeys) {
            dhParameterSpec = diffieHellmanEncryption.getDHParameterSpec(HabboServer.habboConfig.encryptionConfig.diffieHellmanConfig.keySize)

            serverKeyPair = KeyPairGenerator.getInstance("DH", "BC").run {
                initialize(dhParameterSpec)

                genKeyPair()
            }

            serverKeyAgree = KeyAgreement.getInstance("DH", "BC").apply {
                init(serverKeyPair!!.private)
            }
        }
    }

    fun generateDiffieHellmanParameterSpec(): DHParameterSpec {
        return when {
            HabboServer.habboConfig.encryptionConfig.diffieHellmanConfig.alwaysGenerateNewKeys -> diffieHellmanEncryption.getDHParameterSpec(HabboServer.habboConfig.encryptionConfig.diffieHellmanConfig.keySize)
            else -> dhParameterSpec!!
        }
    }

    fun calculateDiffieHellmanSharedKey(diffieHellmanParams: DHParameterSpec, publicKey: String): Pair<BigInteger, BigInteger> {
        val serverKeyPair1: KeyPair
        val serverKeyAgree1: KeyAgreement
        val clientPublicKey = KeyFactory.getInstance("DH", "BC").run {
            generatePublic(DHPublicKeySpec(BigInteger(rsaEncryption.verify(Hex.decode(publicKey)).toString(Charsets.UTF_8)), diffieHellmanParams.p, diffieHellmanParams.g))
        }

        if (HabboServer.habboConfig.encryptionConfig.diffieHellmanConfig.alwaysGenerateNewKeys) {
            serverKeyPair1 = KeyPairGenerator.getInstance("DH", "BC").run {
                initialize(diffieHellmanParams)

                genKeyPair()
            }

            serverKeyAgree1 = KeyAgreement.getInstance("DH", "BC").apply {
                init(serverKeyPair1.private)
            }
        } else {
            serverKeyPair1 = serverKeyPair!!
            serverKeyAgree1 = serverKeyAgree!!
        }

        serverKeyAgree1.doPhase(clientPublicKey, true)

        return (serverKeyPair1.public as DHPublicKey).y to BigInteger(serverKeyAgree1.generateSecret())
    }

    fun getRsaStringEncrypted(bytes: ByteArray): String = Hex.toHexString(rsaEncryption.sign(bytes))
}