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

package ovh.rwx.habbo.encryption.wedgie

import ovh.rwx.habbo.encryption.IHabboEncryption
import ovh.rwx.habbo.encryption.WedgieRC4
import kotlin.math.abs

class HabboRandom(private var seed: Int, private val modulus: Int = 65536) {
    fun nextInt(): Int {
        // Usamos Long para simular o 'Number' do AS3 e evitar o overflow do Int32
        val calc = abs((19979L * seed.toLong()) + 5L)

        seed = (calc % modulus.toLong()).toInt()

        return seed
    }
}

class WedgieWrapper(
    val inner: WedgieRC4,
    val outer: WedgieRC4,
    val rng: HabboRandom
) : IHabboEncryption {
    override fun parse(data: ByteArray): ByteArray = data // Não usado diretamente
}