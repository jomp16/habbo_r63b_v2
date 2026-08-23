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

import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher

class RSAEncryption(n: String, d: String, e: String) {
    private val publicKey: RSAPublicKey
    private val privateKey: RSAPrivateKey

    init {
        val n1 = BigInteger(n, 16)
        val d1 = BigInteger(d, 16)
        val e1 = BigInteger(e, 16)
        val keyFactory = KeyFactory.getInstance("RSA", "BC")

        publicKey = keyFactory.generatePublic(RSAPublicKeySpec(n1, e1)) as RSAPublicKey
        privateKey = keyFactory.generatePrivate(RSAPrivateKeySpec(n1, d1)) as RSAPrivateKey
    }

    // NOTE: Cipher is stateful and NOT thread-safe. A fresh instance is created
    // per call instead of sharing a single field across concurrent handshakes.
    fun sign(src: ByteArray): ByteArray = try {
        Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC").run {
            init(Cipher.ENCRYPT_MODE, privateKey)
            doFinal(src)
        }
    } catch (e: Exception) {
        byteArrayOf()
    }

    @Suppress("unused")
    fun encrypt(src: ByteArray): ByteArray = try {
        Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC").run {
            init(Cipher.ENCRYPT_MODE, publicKey)
            doFinal(src)
        }
    } catch (e: Exception) {
        byteArrayOf()
    }

    fun verify(src: ByteArray): ByteArray = try {
        Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC").run {
            init(Cipher.DECRYPT_MODE, privateKey)
            doFinal(src)
        }
    } catch (e: Exception) {
        byteArrayOf()
    }

    @Suppress("unused")
    fun decrypt(src: ByteArray): ByteArray = try {
        Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC").run {
            init(Cipher.DECRYPT_MODE, publicKey)
            doFinal(src)
        }
    } catch (e: Exception) {
        byteArrayOf()
    }
}
