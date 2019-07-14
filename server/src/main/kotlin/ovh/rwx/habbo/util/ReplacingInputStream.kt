/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

// https://github.com/Inbot/inbot-utils/blob/master/src/main/java/io/inbot/utils/ReplacingInputStream.java
class ReplacingInputStream(inputStream: InputStream, private val pattern: ByteArray, private val replacement: ByteArray) : FilterInputStream(inputStream) {
    private var buf: IntArray = IntArray(pattern.size)
    private var matchedIndex = 0
    private var unbufferIndex = 0
    private var replacedIndex = 0
    private var state = State.NOT_MATCHED

    private enum class State {
        NOT_MATCHED,
        MATCHING,
        REPLACING,
        UNBUFFER
    }

    constructor(inputStream: InputStream, pattern: String, replacement: String) : this(inputStream, pattern.toByteArray(StandardCharsets.UTF_8), replacement.toByteArray(StandardCharsets.UTF_8))

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || len > b.size - off) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return 0
        }

        var c = read()
        if (c == -1) {
            return -1
        }
        b[off] = c.toByte()

        var i = 1
        try {
            while (i < len) {
                c = read()
                if (c == -1) {
                    break
                }
                b[off + i] = c.toByte()
                i++
            }
        } catch (ee: IOException) {
        }

        return i

    }

    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    override fun read(): Int {
        val next: Int
        when (state) {
            State.NOT_MATCHED -> {
                next = super.read()

                return if (pattern[0].toInt() == next) {
                    buf = IntArray(pattern.size)
                    matchedIndex = 0

                    buf[matchedIndex++] = next
                    if (pattern.size == 1) {
                        state = State.REPLACING
                        replacedIndex = 0
                    } else {
                        state = State.MATCHING
                    }
                    read()
                } else {
                    next
                }
            }
            State.MATCHING -> {
                next = super.read()
                if (pattern[matchedIndex].toInt() == next) {
                    buf[matchedIndex++] = next
                    if (matchedIndex == pattern.size) {
                        if (replacement.isEmpty()) {
                            state = State.NOT_MATCHED
                            matchedIndex = 0
                        } else {
                            state = State.REPLACING
                            replacedIndex = 0
                        }
                    }
                } else {
                    buf[matchedIndex++] = next
                    state = State.UNBUFFER
                    unbufferIndex = 0
                }

                return read()
            }
            State.REPLACING -> {
                next = replacement[replacedIndex++].toInt()

                if (replacedIndex == replacement.size) {
                    state = State.NOT_MATCHED
                    replacedIndex = 0
                }

                return next
            }
            State.UNBUFFER -> {
                next = buf[unbufferIndex++]

                if (unbufferIndex == matchedIndex) {
                    state = State.NOT_MATCHED
                    matchedIndex = 0
                }

                return next
            }
            else -> throw IllegalStateException("no such state $state")
        }
    }

    override fun toString(): String {
        return state.name + " " + matchedIndex + " " + replacedIndex + " " + unbufferIndex
    }
}