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

package tk.jomp16.habbo.kotlin

import tk.jomp16.habbo.util.Utils
import java.time.LocalDateTime

fun localDateTimeNowWithoutSecondsAndNanos(): LocalDateTime = LocalDateTime.now().withNano(0).withSecond(0)

inline fun <reified INNER> array2d(sizeOuter: Int, sizeInner: Int, noinline innerInit: (Int) -> INNER) = Array(
        sizeOuter) { Array(sizeInner, innerInit) }

fun array2dOfShort(sizeOuter: Int, sizeInner: Int) = Array(sizeOuter) { ShortArray(sizeInner) }

fun array2dOfInt(sizeOuter: Int, sizeInner: Int) = Array(sizeOuter) { IntArray(sizeInner) }

fun array2dOfLong(sizeOuter: Int, sizeInner: Int) = Array(sizeOuter) { LongArray(sizeInner) }

fun array2dOfByte(sizeOuter: Int, sizeInner: Int) = Array(sizeOuter) { ByteArray(sizeInner) }

fun array2dOfChar(sizeOuter: Int, sizeInner: Int) = Array(sizeOuter) { CharArray(sizeInner) }

fun <E> List<E>.random(): E = this[Utils.random.nextInt(this.size)]