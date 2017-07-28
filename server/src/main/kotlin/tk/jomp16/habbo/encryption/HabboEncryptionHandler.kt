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

import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import javax.crypto.KeyAgreement
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.DHPublicKeySpec
import javax.xml.bind.DatatypeConverter

class HabboEncryptionHandler(n: String, d: String, e: String) {
    val rsaEncryption: RSAEncryption = RSAEncryption(n, d, e)

    fun calculateDiffieHellmanSharedKey(diffieHellmanParams: DHParameterSpec, publicKey: String): Pair<BigInteger, BigInteger> {
        val serverKeyPair = KeyPairGenerator.getInstance("DH", "BC").run {
            initialize(diffieHellmanParams)

            genKeyPair()
        }
        val serverKeyAgree = KeyAgreement.getInstance("DH", "BC").apply {
            init(serverKeyPair.private)
        }
        val clientPublicKey = KeyFactory.getInstance("DH", "BC").run {
            generatePublic(DHPublicKeySpec(BigInteger(rsaEncryption.verify(DatatypeConverter.parseHexBinary(publicKey)).toString(Charsets.UTF_8)), diffieHellmanParams.p, diffieHellmanParams.g))
        }

        serverKeyAgree.doPhase(clientPublicKey, true)

        return (serverKeyPair.public as DHPublicKey).y to BigInteger(serverKeyAgree.generateSecret())
    }

    fun getRsaStringEncrypted(bytes: ByteArray): String = DatatypeConverter.printHexBinary(rsaEncryption.sign(bytes))
}