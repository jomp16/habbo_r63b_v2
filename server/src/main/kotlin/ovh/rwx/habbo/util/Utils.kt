/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ovh.rwx.habbo.HabboServer
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URL
import java.security.SecureRandom
import java.util.*
import java.util.zip.Deflater
import java.util.zip.Inflater

@Suppress("unused")
object Utils {
    val random: SecureRandom = SecureRandom()
    private val ramUsage: Long
        get() = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    val ramUsageString: String
        get() = humanReadableByteCount(ramUsage, true)
    private const val ipInfoApiURL: String = "http://api.ipstack.com/"

    fun humanReadableByteCount(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024

        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + (if (si) "" else "i")

        return "%.1f %sB".format(bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    fun randInt(range: IntRange, random1: Random = random): Int = random1.nextInt((range.endInclusive - range.start) + 1) + range.start

    fun round(value: Double, places: Int): Double {
        if (places < 0) throw IllegalArgumentException()

        return BigDecimal(value).setScale(places, RoundingMode.HALF_UP).toDouble()
    }

    fun inflate(source: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(source)

        ByteArrayOutputStream(source.size).use { byteArrayOutputStream ->
            while (!inflater.finished()) {
                val buff = ByteArray(DEFAULT_BUFFER_SIZE)
                val count = inflater.inflate(buff)

                byteArrayOutputStream.write(buff, 0, count)
            }

            return byteArrayOutputStream.toByteArray()
        }
    }

    fun deflate(source: ByteArray): ByteArray = deflate(source, Deflater.DEFAULT_COMPRESSION)

    @Suppress("MemberVisibilityCanBePrivate")
    fun deflate(source: ByteArray, compression: Int): ByteArray {
        val deflater = Deflater(compression)
        deflater.setInput(source)
        deflater.finish()

        ByteArrayOutputStream(source.size).use { byteArrayOutputStream ->
            while (!deflater.finished()) {
                val buff = ByteArray(DEFAULT_BUFFER_SIZE)
                val count = deflater.deflate(buff)

                byteArrayOutputStream.write(buff, 0, count)
            }

            return byteArrayOutputStream.toByteArray()
        }
    }

    fun getIpInfo(ipOrDomain: String): IpInfo {
        val url = URL("$ipInfoApiURL/$ipOrDomain?access_key=${HabboServer.habboConfig.analyticsConfig.ipConfig.apiKey}&hostname=1")

        url.openStream().use {
            return jacksonObjectMapper().readValue(it)
        }
    }
}