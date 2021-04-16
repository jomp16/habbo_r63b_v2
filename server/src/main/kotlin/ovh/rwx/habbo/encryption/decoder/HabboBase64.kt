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

}