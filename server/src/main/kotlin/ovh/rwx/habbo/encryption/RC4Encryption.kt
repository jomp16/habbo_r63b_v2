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

/**
 * RC4 contínuo (o estado do keystream atravessa os pacotes). Implementação própria no lugar do `RC4Engine`
 * do BouncyCastle: o laço mantém o estado em variáveis locais (nada de campos a cada byte) e processa no lugar,
 * já que a cifra de saída roda para CADA sessão a CADA pacote (era ~25% da CPU num quarto com 200 usuários).
 * O resultado é byte a byte idêntico ao do BouncyCastle (coberto em `RC4EncryptionTest`).
 *
 * Não é thread-safe: cada sessão usa a sua instância, sempre da mesma thread (o event loop do canal).
 */
class RC4Encryption(key: ByteArray) : IHabboEncryption {
    private val state = IntArray(256) { it }
    private var x = 0
    private var y = 0

    init {
        require(key.isNotEmpty()) { "chave RC4 vazia" }

        var j = 0
        for (i in 0 until 256) {
            j = (j + state[i] + (key[i % key.size].toInt() and 0xFF)) and 0xFF
            val tmp = state[i]
            state[i] = state[j]
            state[j] = tmp
        }
    }

    override fun parse(data: ByteArray): ByteArray {
        val out = data.copyOf()
        parseInPlace(out, 0, out.size)
        return out
    }

    override fun parseInPlace(data: ByteArray, offset: Int, length: Int) {
        val s = state
        var i = x
        var j = y

        for (k in offset until offset + length) {
            i = (i + 1) and 0xFF
            val si = s[i]
            j = (j + si) and 0xFF
            val sj = s[j]
            s[i] = sj
            s[j] = si
            data[k] = (data[k].toInt() xor s[(si + sj) and 0xFF]).toByte()
        }

        x = i
        y = j
    }
}