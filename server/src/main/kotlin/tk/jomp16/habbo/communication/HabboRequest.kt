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

package tk.jomp16.habbo.communication

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.util.ReferenceCountUtil
import java.io.Closeable

class HabboRequest(val headerId: Int, val byteBuf: ByteBuf) : Closeable {
    private val byteBufInputStream: ByteBufInputStream = ByteBufInputStream(byteBuf)

    fun readUTF(): String = if (byteBuf.readableBytes() < 2) "" else byteBufInputStream.readUTF()

    @Suppress("unused")
    fun readShort(): Short = if (byteBuf.readableBytes() < 2) 0 else byteBufInputStream.readShort()

    fun readInt(): Int = if (byteBuf.readableBytes() < 4) 0 else byteBufInputStream.readInt()

    fun readBoolean(): Boolean = byteBuf.readableBytes() >= 1 && byteBufInputStream.readBoolean()

    override fun toString(): String {
        var message = byteBuf.toString(Charsets.UTF_8).replace("[\\r\\n]+", "(newline)")

        for (i in 0..31) {
            message = message.replace(i.toChar().toString(), "[$i]")
        }

        return message
    }

    override fun close() {
        byteBufInputStream.close()

        byteBuf.readerIndex(byteBuf.readableBytes())
        byteBuf.discardSomeReadBytes()

        ReferenceCountUtil.release(byteBuf)
    }
}