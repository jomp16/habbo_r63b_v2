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

interface IHabboEncryption {
    fun parse(data: ByteArray): ByteArray

    /**
     * Processa `data[offset, offset+length)` NO LUGAR, sem alocar. A implementação padrão cai em [parse]
     * (que aloca); as de caminho quente (RC4 de saída) sobrescrevem.
     */
    fun parseInPlace(data: ByteArray, offset: Int, length: Int) {
        val result = parse(data.copyOfRange(offset, offset + length))
        System.arraycopy(result, 0, data, offset, length)
    }
}