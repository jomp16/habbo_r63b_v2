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

package ovh.rwx.habbo.encryption

import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher

class RSAEncryption(n: String, d: String, e: String) {
    private val cipher: Cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC")
    private var publicKey: RSAPublicKey
    private var privateKey: RSAPrivateKey

    init {
        val n1 = BigInteger(n, 16)
        val d1 = BigInteger(d, 16)
        val e1 = BigInteger(e, 16)
        val keyFactory = KeyFactory.getInstance("RSA", "BC")
        val pubKeySpec = RSAPublicKeySpec(n1, e1)
        val privKeySpec = RSAPrivateKeySpec(n1, d1)

        publicKey = keyFactory.generatePublic(pubKeySpec) as RSAPublicKey
        privateKey = keyFactory.generatePrivate(privKeySpec) as RSAPrivateKey
    }

    fun sign(src: ByteArray): ByteArray {
        return try {
            cipher.init(Cipher.ENCRYPT_MODE, privateKey)

            cipher.doFinal(src)
        } catch (e: Exception) {
            byteArrayOf()
        }
    }

    @Suppress("unused")
    fun encrypt(src: ByteArray): ByteArray {
        return try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)

            cipher.doFinal(src)
        } catch (e: Exception) {
            byteArrayOf()
        }
    }

    fun verify(src: ByteArray): ByteArray {
        return try {
            cipher.init(Cipher.DECRYPT_MODE, privateKey)

            cipher.doFinal(src)
        } catch (e: Exception) {
            byteArrayOf()
        }
    }

    @Suppress("unused")
    fun decrypt(src: ByteArray): ByteArray {
        return try {
            cipher.init(Cipher.DECRYPT_MODE, publicKey)

            cipher.doFinal(src)
        } catch (e: Exception) {
            byteArrayOf()
        }
    }
}