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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Security
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAKeyGenParameterSpec
import java.security.spec.RSAPrivateKeySpec
import javax.crypto.Cipher

/**
 * A chave privada em forma CRT (primos recuperados de N, D, E) TEM de produzir exatamente o mesmo resultado da chave
 * comum (N, D): é só uma forma mais rápida de calcular a mesma exponenciação.
 */
class RSACrtTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun registerProvider() {
            if (Security.getProvider("BC") == null) Security.addProvider(BouncyCastleProvider())
        }
    }

    private class Keys(val n: BigInteger, val d: BigInteger, val e: BigInteger, val plainPrivate: RSAPrivateKey, val publicKey: RSAPublicKey)

    private fun newKeys(bits: Int, e: Long): Keys {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(RSAKeyGenParameterSpec(bits, BigInteger.valueOf(e))) }.generateKeyPair()
        val pub = pair.public as RSAPublicKey
        val priv = pair.private as RSAPrivateKey
        // chave privada SEM CRT, de referência (só N e D), exatamente como o código antigo a montava
        val plain = KeyFactory.getInstance("RSA", "BC").generatePrivate(RSAPrivateKeySpec(pub.modulus, priv.privateExponent)) as RSAPrivateKey
        return Keys(pub.modulus, priv.privateExponent, pub.publicExponent, plain, pub)
    }

    @Test
    fun `recupera os primos de N a partir de D e E`() {
        for ((bits, e) in listOf(512 to 3L, 1024 to 3L, 2048 to 3L, 2048 to 65537L)) {
            val k = newKeys(bits, e)

            val factors = recoverPrimeFactors(k.n, k.e, k.d)

            assertNotNull("sem fatoração para $bits bits, e=$e", factors)
            assertEquals(k.n, factors!!.first.multiply(factors.second))
            assertTrue(factors.first > BigInteger.ONE && factors.second > BigInteger.ONE)
        }
    }

    @Test
    fun `chave inconsistente nao fatora`() {
        val k = newKeys(1024, 3)

        assertNull(recoverPrimeFactors(k.n, k.e, k.d.add(BigInteger.ONE))) // D adulterado
    }

    @Test
    fun `sign com CRT e identico ao da chave comum`() {
        for ((bits, e) in listOf(1024 to 3L, 2048 to 3L, 2048 to 65537L)) {
            val k = newKeys(bits, e)
            val crt = RSAEncryption(k.n.toString(16), k.d.toString(16), k.e.toString(16))
            val reference = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC").apply { init(Cipher.ENCRYPT_MODE, k.plainPrivate) }

            repeat(20) {
                // até 80 bytes: o bloco PKCS#1 v1.5 de uma chave de 1024 bits comporta 117
                val input = (1..(1 + it * 4)).map { i -> (i * 31 + it).toByte() }.toByteArray()
                assertArrayEquals("sign divergiu ($bits bits, e=$e)", reference.doFinal(input), crt.sign(input))
            }
        }
    }

    @Test
    fun `verify com CRT decifra o que a chave publica cifrou`() {
        val k = newKeys(2048, 3)
        val crt = RSAEncryption(k.n.toString(16), k.d.toString(16), k.e.toString(16))
        val publicCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC").apply { init(Cipher.ENCRYPT_MODE, k.publicKey) }

        repeat(20) {
            val plain = ByteArray(40 + it) { i -> (i + it).toByte() }
            assertArrayEquals(plain, crt.verify(publicCipher.doFinal(plain)))
        }
    }
}
