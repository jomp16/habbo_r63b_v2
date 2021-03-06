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

package ovh.rwx.habbo.communication

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.util.ReferenceCountUtil
import ovh.rwx.habbo.communication.incoming.Incoming

@Suppress("unused")
class HabboRequest(val headerId: Int, val byteBuf: ByteBuf) : AutoCloseable {
    private val byteBufInputStream: ByteBufInputStream = ByteBufInputStream(byteBuf)
    lateinit var incoming: Incoming
    lateinit var methodName: String

    fun readUTF(): String = if (byteBuf.readableBytes() < 2) "" else byteBufInputStream.readUTF()

    fun readShort(): Short = if (byteBuf.readableBytes() < 2) 0 else byteBufInputStream.readShort()

    fun readInt(): Int = if (byteBuf.readableBytes() < 4) 0 else byteBufInputStream.readInt()

    fun readBoolean(): Boolean = byteBuf.readableBytes() >= 1 && byteBufInputStream.readBoolean()

    fun readBytes(bytes: ByteArray) = byteBufInputStream.readFully(bytes)

    override fun toString(): String {
        var message = byteBuf.toString(Charsets.UTF_8).replace("[\\r\\n]+", "(newline)")

        for (i in 0..31) message = message.replace(i.toChar().toString(), "[$i]")

        return message
    }

    override fun close() {
        byteBufInputStream.close()

        byteBuf.readerIndex(byteBuf.readableBytes())
        byteBuf.discardSomeReadBytes()

        ReferenceCountUtil.release(byteBuf)
    }
}