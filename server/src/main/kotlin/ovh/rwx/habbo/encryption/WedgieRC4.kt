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

class WedgieRC4(key: ByteArray) : IHabboEncryption {
    private val sBox = IntArray(256)
    private var i = 0
    private var j = 0

    init {
        // 1. XOR Inicial (Idêntico ao AS3 init)
        val salt = "mWxFRJnGJ5T9Si0OMVvEBBm8laihXkN8GmH6fuv7ldZhLyGRRKCcGzziPYBaJom".toByteArray()
        val saltedKey = ByteArray(key.size) { k ->
            (key[k].toInt() xor salt[k % salt.size].toInt()).toByte()
        }

        // 2. KSA
        for (k in 0..255) sBox[k] = k
        var tempJ = 0
        for (k in 0..255) {
            tempJ = (tempJ + sBox[k] + (saltedKey[k % saltedKey.size].toInt() and 0xFF)) % 256
            swap(k, tempJ)
        }

        // 3. PREMIX (52 Rounds sobre a string fixa)
        val premixString =
            "NV6VVFPoC7FLDlzDUri3qcOAg9cRoFOmsYR9ffDGy5P8HfF6eekX40SFSVfJ1mDb3lcpYRqdg28sp61eHkPukKbqTu1JsVEKiRavi04YtSzUsLXaYSa5BEGwg5G2OF".toByteArray()
        repeat(52) {
            parse(premixString)
        }
    }

    private fun swap(a: Int, b: Int) {
        val temp = sBox[a]
        sBox[a] = sBox[b]
        sBox[b] = temp
    }

    override fun parse(data: ByteArray): ByteArray {
        val out = ByteArray(data.size)
        for (k in data.indices) {
            i = (i + 1) % 256
            j = (j + sBox[i]) % 256
            swap(i, j)

            // SHUFFLE STEP (§_-1zc§ no seu AS3)
            if ((i and 0x3F) == 63) {
                val m = ((i + 67) * 297) and 0xFF
                val n = (j + sBox[m]) and 0xFF
                swap(m, n)
            }

            val t = (sBox[i] + sBox[j]) % 256
            out[k] = (data[k].toInt() xor sBox[t]).toByte()
        }
        return out
    }
}