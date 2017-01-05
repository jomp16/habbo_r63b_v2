/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.kotlin

import tk.jomp16.habbo.util.Utils
import java.net.URL
import java.net.URLConnection
import java.time.Clock
import java.time.LocalDateTime

fun localDateTimeNowWithoutSecondsAndNanos(): LocalDateTime = LocalDateTime.now(Clock.systemUTC()).withNano(0).withSecond(0)

inline fun <reified INNER> array2d(sizeOuter: Int, sizeInner: Int, noinline innerInit: (Int) -> INNER) = Array(sizeOuter) { Array(sizeInner, innerInit) }

fun array2dOfShort(sizeOuter: Int, sizeInner: Int) = Array(sizeOuter) { ShortArray(sizeInner) }

fun <E> List<E>.random(): E = this[Utils.random.nextInt(this.size)]

fun urlUserAgent(url: String, userAgent: String = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36"): URLConnection {
    val urlConnection = URL(url).openConnection()
    urlConnection.setRequestProperty("User-Agent", userAgent)

    return urlConnection
}