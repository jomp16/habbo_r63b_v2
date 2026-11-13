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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ovh.rwx.habbo.HabboServer
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

@Suppress("unused")
object Utils {
    private val ramUsage: Long
        get() = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    val ramUsageString: String
        get() = humanReadableByteCount(ramUsage, true)
    private const val ipInfoApiURL: String = "http://api.ipstack.com/"

    fun humanReadableByteCount(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024

        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + (if (si) "" else "i")

        return "%.1f %sB".format(bytes / unit.toDouble().pow(exp.toDouble()), pre)
    }

    fun round(value: Double, places: Int): Double {
        if (places < 0) throw IllegalArgumentException()

        // Caminho rápido sem BigDecimal (era ~11% da alocação em carga): só vale longe de um empate .5, onde o
        // arredondamento do double não pode divergir do HALF_UP exato; perto do empate, volta ao BigDecimal.
        if (places <= 9 && value.isFinite() && abs(value) < 1e9) {
            val factor = POW10[places]
            val scaled = value * factor
            val floor = floor(scaled)
            val frac = scaled - floor
            if (abs(frac - 0.5) > 1e-6) {
                val rounded = if (frac > 0.5) floor + 1.0 else floor
                return rounded / factor
            }
        }

        return BigDecimal(value).setScale(places, RoundingMode.HALF_UP).toDouble()
    }

    private val POW10 = DoubleArray(10).also { var p = 1.0; for (i in it.indices) { it[i] = p; p *= 10.0 } }

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
        val url =
            URI.create("$ipInfoApiURL/$ipOrDomain?access_key=${HabboServer.habboConfig.analyticsConfig.ipConfig.apiKey}&hostname=1")
                .toURL()

        url.openStream().use {
            return jacksonObjectMapper().readValue(it)
        }
    }

    fun isTestEnvironment(): Boolean {
        if (System.getProperty("habbo.allow.test.server") == "true") return false
        if (System.getProperty("org.gradle.test.worker") != null) return true
        if (System.getProperty("idea.test.cyclic.buffer.size") != null) return true
        return Thread.getAllStackTraces().values.any { stackTrace ->
            stackTrace.any { frame ->
                val c = frame.className
                c.startsWith("org.junit.") ||
                        c.startsWith("org.testng.") ||
                        c.startsWith("com.intellij.rt.junit.") ||
                        c.startsWith("com.intellij.rt.execution.junit.") ||
                        c.startsWith("org.gradle.api.internal.tasks.testing.")
            }
        }
    }
}

