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

package ovh.rwx.habbo.util

object BytePatterns {
    /**
     * Cópia de [bytes] sem nenhuma ocorrência de [patterns], numa só passada (o primeiro padrão que casa numa posição
     * é removido; a busca segue logo depois dele, sem casar sobre o trecho removido). Sem ocorrências devolve o
     * próprio [bytes], sem copiar.
     *
     * Substitui o antigo `ReplacingInputStream`, que lia byte a byte (uma chamada por byte em cada camada, 3 camadas
     * sobre a furnidata de ~13 MB): segundos de CPU numa VPS pequena.
     */
    fun removeAll(bytes: ByteArray, patterns: List<ByteArray>): ByteArray {
        val nonEmpty = patterns.filter { it.isNotEmpty() }
        if (nonEmpty.isEmpty()) return bytes

        val firstBytes = BooleanArray(256)
        nonEmpty.forEach { firstBytes[it[0].toInt() and 0xFF] = true }

        var out: ByteArray? = null
        var outSize = 0
        var copiedUntil = 0
        var i = 0

        while (i < bytes.size) {
            if (!firstBytes[bytes[i].toInt() and 0xFF]) {
                i++
                continue
            }

            val match = nonEmpty.firstOrNull { matchesAt(bytes, i, it) }
            if (match == null) {
                i++
                continue
            }

            val target = out ?: ByteArray(bytes.size).also { out = it }
            System.arraycopy(bytes, copiedUntil, target, outSize, i - copiedUntil)
            outSize += i - copiedUntil
            i += match.size
            copiedUntil = i
        }

        val target = out ?: return bytes
        System.arraycopy(bytes, copiedUntil, target, outSize, bytes.size - copiedUntil)
        outSize += bytes.size - copiedUntil

        return target.copyOf(outSize)
    }

    private fun matchesAt(bytes: ByteArray, at: Int, pattern: ByteArray): Boolean {
        if (at + pattern.size > bytes.size) return false
        for (j in pattern.indices) {
            if (bytes[at + j] != pattern[j]) return false
        }

        return true
    }
}
