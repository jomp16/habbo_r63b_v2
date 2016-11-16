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

import tk.jomp16.habbo.util.Utils
import java.math.BigInteger

class DiffieHellmanEncryption {
    var privateKey: BigInteger = BigInteger.ZERO
    var publicKey: BigInteger = BigInteger.ZERO
    var prime: BigInteger = BigInteger.ZERO
    var generator: BigInteger = BigInteger.ZERO

    init {
        initialize()
    }

    private fun initialize() {
        while (publicKey == BigInteger.ZERO) {
            prime = BigInteger.probablePrime(BIT_LENGTH, Utils.random)
            generator = BigInteger.probablePrime(BIT_LENGTH, Utils.random)

            if (!prime.isProbablePrime(10) || !generator.isProbablePrime(10)) continue

            val bytes = ByteArray(BIT_LENGTH / 8)

            Utils.random.nextBytes(bytes)

            privateKey = BigInteger(bytes)

            if (generator.compareTo(prime) == 1) {
                val tmp = prime

                prime = generator
                generator = tmp
            }

            publicKey = generator.modPow(privateKey, prime)
        }
    }

    fun calculateSharedKey(m: BigInteger): BigInteger = m.modPow(privateKey, prime)

    companion object {
        const val BIT_LENGTH = 32
    }
}