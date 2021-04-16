package ovh.rwx.habbo.encryption.decoder

import kotlin.math.abs

object HabboVl64 {
    fun encodeBytes(i: Int): ByteArray? {
        var number = i
        val wf = ByteArray(6)
        var pos = 0
        val startPos = pos
        var bytes = 1
        val negativeMask = if (number >= 0) 0 else 4
        number = abs(number)
        wf[pos++] = (64 + (number and 3)).toByte()
        number = number shr 2
        while (number != 0) {
            bytes++
            wf[pos++] = (64 + (number and 63)).toByte()
            number = number shr 6
        }

        wf[startPos] = (wf[startPos].toInt() or (bytes shl 3) or negativeMask).toByte()
        return wf.copyOf(bytes)
    }

    fun decode(raw: ByteArray): IntArray {
        return try {
            var pos = 0
            var v: Int
            val negative = raw[pos].toInt() and 4 == 4
            val totalBytes: Int = raw[pos].toInt() shr 3 and 7
            v = raw[pos].toInt() and 3
            pos++
            var shiftAmount = 2
            for (b in 1 until totalBytes) {
                v = v or (raw[pos].toInt() and 63 shl shiftAmount)
                shiftAmount = 2 + 6 * b
                pos++
            }
            if (negative) {
                v *= -1
            }
            intArrayOf(v, totalBytes)
        } catch (e: Exception) {
            // ignore
            intArrayOf(0, 0)
        }
    }
}