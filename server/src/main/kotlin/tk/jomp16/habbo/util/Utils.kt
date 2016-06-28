/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.SecureRandom

object Utils {
    val random: SecureRandom = SecureRandom()

    private val hexArray: CharArray = "0123456789ABCDEF".toCharArray()

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)

        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 255

            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 15]
        }

        return String(hexChars)
    }

    private val ramUsage: Long
        get() = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    val ramUsageString: String
        get() = humanReadableByteCount(ramUsage, true)

    private fun humanReadableByteCount(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024

        if (bytes < unit) return "$bytes B"

        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + (if (si) "" else "i")

        return "%.1f %sB".format(bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    fun randInt(range: IntRange): Int = random.nextInt((range.endInclusive - range.start) + 1) + range.start

    fun round(value: Double, places: Int): Double {
        if (places < 0) throw IllegalArgumentException()

        return BigDecimal(value).setScale(places, RoundingMode.HALF_UP).toDouble()
    }
}