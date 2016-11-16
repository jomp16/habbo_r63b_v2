/*
 * Copyright (C) 2016 jomp16
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

class RC4Encryption(key: ByteArray) {
    private var bytes = ByteArray(POOL_SIZE)

    private var i = 0
    private var j = 0

    init {
        i = 0

        while (i < POOL_SIZE) {
            bytes[i] = i.toByte()

            ++i
        }

        i = 0

        while (i < POOL_SIZE) {
            j = j + bytes[i].toInt() + key[i % key.size].toInt() and POOL_SIZE - 1

            swap(i, j)

            ++i
        }

        i = 0
        j = 0
    }

    private fun next(): Byte {
        i = ++i and POOL_SIZE - 1
        j = j + bytes[i] and POOL_SIZE - 1

        swap(i, j)

        return bytes[bytes[i] + bytes[j] and 255]
    }

    fun parse(src: ByteArray) {
        src.indices.forEach { k -> src[k] = (src[k].toInt() xor next().toInt()).toByte() }
    }

    private fun swap(a: Int, b: Int) {
        val t = bytes[a]

        bytes[a] = bytes[b]
        bytes[b] = t
    }

    companion object {
        private const val POOL_SIZE: Int = 256
    }
}