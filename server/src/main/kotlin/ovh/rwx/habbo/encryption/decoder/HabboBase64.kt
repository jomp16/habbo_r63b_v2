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

package ovh.rwx.habbo.encryption.decoder

import kotlin.math.pow

object HabboBase64 {
    fun encodeBytes(i: Int): ByteArray {
        val tmp = ByteArray(2)

        (1..2).forEach { j ->
            tmp[j - 1] = (64 + (i shr 6 * (2 - j) and 63)).toByte()
        }

        return tmp
    }

    fun decode(bytes: ByteArray): Int {
        return try {
            var intTot = 0
            (bytes.size - 1 downTo 0).withIndex().forEach { (y, x) ->
                var intTmp = (bytes[x] - 64).toByte().toInt()
                if (y > 0) intTmp *= 64.0.pow(y.toDouble()).toInt()

                intTot += intTmp
            }

            intTot
        } catch (e: Exception) {
            // ignore
            0
        }
    }

    /**
     * Codifica um inteiro em um array de bytes Base64 do Habbo com tamanho variável.
     * Exemplo: encode(payloadSize, 3) retorna 3 bytes.
     */
    fun encode(i: Int, numBytes: Int): ByteArray {
        val bzRes = ByteArray(numBytes)
        for (j in 1..numBytes) {
            val calc = (numBytes - j) * 6
            bzRes[j - 1] = (64 + ((i shr calc) and 63)).toByte()
        }
        return bzRes
    }
}