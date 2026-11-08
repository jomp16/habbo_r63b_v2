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
import java.security.spec.RSAPrivateCrtKeySpec
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

        // A config só traz N, D e E. Recuperamos p e q (N = p*q) para usar a forma CRT da chave privada: duas
        // exponenciações de metade do tamanho em vez de uma inteira (~3-4x mais rápido; o resultado é idêntico).
        // Se por algum motivo não for possível fatorar, cai na chave comum (N, D) — só mais lenta.
        val crtKey = recoverPrimeFactors(n1, e1, d1)?.let { (p0, q0) ->
            val p = maxOf(p0, q0)
            val q = minOf(p0, q0)
            keyFactory.generatePrivate(
                RSAPrivateCrtKeySpec(n1, e1, d1, p, q, d1.mod(p - BigInteger.ONE), d1.mod(q - BigInteger.ONE), q.modInverse(p))
            )
        }

        privateKey = (crtKey ?: keyFactory.generatePrivate(RSAPrivateKeySpec(n1, d1))) as RSAPrivateKey
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

/**
 * Recupera os primos `p` e `q` (N = p*q) a partir de `(N, D, E)`, com o algoritmo clássico (NIST SP 800-56B, anexo C):
 * `k = D*E - 1` é múltiplo de λ(N); escreve-se `k = 2^t * r` (r ímpar) e, para um `g` qualquer, a sequência
 * `g^r, g^(2r), ... g^k = 1` passa por uma raiz quadrada não trivial de 1, cujo `gcd(x - 1, N)` é um fator de N.
 * Cada `g` funciona com probabilidade >= 1/2; o gerador é determinístico (só há fatoração, nenhum segredo novo).
 * Devolve `null` se não achar (chave inconsistente).
 */
internal fun recoverPrimeFactors(n: BigInteger, e: BigInteger, d: BigInteger): Pair<BigInteger, BigInteger>? {
    val k = d.multiply(e).subtract(BigInteger.ONE)
    if (k.signum() <= 0 || k.testBit(0)) return null

    val t = k.lowestSetBit
    val r = k.shiftRight(t)
    val nMinusOne = n.subtract(BigInteger.ONE)
    val random = java.util.Random(0x5EED)

    repeat(256) {
        val g = BigInteger(n.bitLength(), random).mod(n.subtract(BigInteger.valueOf(3))).add(BigInteger.TWO)
        var x = g.modPow(r, n)

        if (x == BigInteger.ONE || x == nMinusOne) return@repeat

        for (i in 1..t) {
            val y = x.multiply(x).mod(n)

            if (y == BigInteger.ONE) {
                val p = x.subtract(BigInteger.ONE).gcd(n)
                if (p != BigInteger.ONE && p != n) return p to n.divide(p)
                break
            }
            if (y == nMinusOne) break

            x = y
        }
    }

    return null
}
