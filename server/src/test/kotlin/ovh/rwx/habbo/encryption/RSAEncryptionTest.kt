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

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Security
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

/**
 * O cache de assinaturas do handshake (`HabboEncryptionHandler.getRsaStringEncrypted`) só é correto porque a
 * assinatura RSA PKCS#1 v1.5 é DETERMINÍSTICA: mesma chave + mesmos bytes = mesma saída. Este teste trava essa premissa.
 */
class RSAEncryptionTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun registerProvider() {
            if (Security.getProvider("BC") == null) Security.addProvider(BouncyCastleProvider())
        }
    }

    private fun newRsa(): RSAEncryption {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(512) }.generateKeyPair()
        val pub = pair.public as RSAPublicKey
        val priv = pair.private as RSAPrivateKey
        return RSAEncryption(pub.modulus.toString(16), priv.privateExponent.toString(16), pub.publicExponent.toString(16))
    }

    @Test
    fun `assinatura com a mesma chave e os mesmos bytes e sempre identica`() {
        val rsa = newRsa()
        val input = "12345678901234567890".toByteArray()

        val first = rsa.sign(input)

        assertTrue(first.isNotEmpty())
        repeat(5) { assertArrayEquals(first, rsa.sign(input)) }
    }

    @Test
    fun `entradas diferentes geram assinaturas diferentes e a chave publica recupera o original`() {
        val rsa = newRsa()
        val a = "111111".toByteArray()
        val b = "222222".toByteArray()

        assertFalse(rsa.sign(a).contentEquals(rsa.sign(b)))
        // decrypt = chave pública: é o que o cliente faz para ler a assinatura do servidor
        assertArrayEquals(a, rsa.decrypt(rsa.sign(a)))
        assertArrayEquals(b, rsa.decrypt(rsa.sign(b)))
    }
}
