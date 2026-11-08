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

import org.bouncycastle.crypto.engines.RC4Engine
import org.bouncycastle.crypto.params.KeyParameter
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import kotlin.random.Random

/**
 * O RC4 próprio (laço com estado local, no lugar) tem de produzir EXATAMENTE o keystream do BouncyCastle que ele
 * substituiu — o cliente decifra com RC4 padrão, qualquer divergência dessincroniza a conexão.
 */
class RC4EncryptionTest {
    private fun bouncyCastle(key: ByteArray) = RC4Engine().apply { init(true, KeyParameter(key)) }

    private fun RC4Engine.run(data: ByteArray): ByteArray = ByteArray(data.size).also { processBytes(data, 0, data.size, it, 0) }

    @Test
    fun `parse e identico ao BouncyCastle com estado continuo entre pacotes`() {
        val random = Random(42)

        repeat(50) {
            val key = random.nextBytes(random.nextInt(1, 64))
            val reference = bouncyCastle(key)
            val ours = RC4Encryption(key)

            // vários pacotes de tamanhos variados (incluindo 0 e acima de 256, que dá a volta nos índices)
            repeat(20) {
                val packet = random.nextBytes(random.nextInt(0, 1500))
                assertArrayEquals(reference.run(packet), ours.parse(packet))
            }
        }
    }

    @Test
    fun `parseInPlace e identico ao BouncyCastle e respeita offset e length`() {
        val random = Random(7)
        val key = random.nextBytes(16)
        val reference = bouncyCastle(key)
        val ours = RC4Encryption(key)

        repeat(30) {
            val payload = random.nextBytes(random.nextInt(1, 600))
            val padded = ByteArray(payload.size + 10) { 0x55 }
            payload.copyInto(padded, 5)

            ours.parseInPlace(padded, 5, payload.size)

            assertArrayEquals(reference.run(payload), padded.copyOfRange(5, 5 + payload.size))
            // bytes fora de [offset, offset+length) não podem ser tocados
            assertArrayEquals(ByteArray(5) { 0x55 }, padded.copyOfRange(0, 5))
            assertArrayEquals(ByteArray(5) { 0x55 }, padded.copyOfRange(5 + payload.size, padded.size))
        }
    }

    @Test
    fun `parse nao altera o array de entrada`() {
        val key = byteArrayOf(1, 2, 3, 4, 5)
        val input = byteArrayOf(10, 20, 30, 40)
        val copy = input.copyOf()

        RC4Encryption(key).parse(input)

        assertArrayEquals(copy, input)
    }

    @Test
    fun `cifrar e decifrar com duas instancias da mesma chave devolve o original`() {
        val random = Random(1)
        val key = random.nextBytes(20)
        val encrypt = RC4Encryption(key)
        val decrypt = RC4Encryption(key)

        repeat(10) {
            val plain = random.nextBytes(random.nextInt(1, 800))
            val buf = plain.copyOf()

            encrypt.parseInPlace(buf, 0, buf.size)
            decrypt.parseInPlace(buf, 0, buf.size)

            assertArrayEquals(plain, buf)
        }
    }
}
