/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.PooledByteBufAllocator
import io.netty.util.ReferenceCountUtil
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.encryption.decoder.HabboVl64

@Suppress("unused")
class HabboResponse(
    val headerId: Int,
    val outgoing: Outgoing?,
    val keepCopy: Boolean = false,
    val outgoingR63A: OutgoingR63A? = null
) : AutoCloseable {
    private val _byteBuf: ByteBuf = PooledByteBufAllocator.DEFAULT.buffer()
    private val byteBufOutputStream: ByteBufOutputStream = ByteBufOutputStream(_byteBuf)
    val byteBuf: ByteBuf
        get() = if (keepCopy) _byteBuf.duplicate() else _byteBuf

    fun writeUTF(s: String, breakChar: Int = 2) {
        if (outgoingR63A != null) {
            byteBufOutputStream.writeBytes(s)
            byteBufOutputStream.writeByte(breakChar)
        } else {
            byteBufOutputStream.writeUTF(s)
        }
    }
    
    fun writeUTFWithoutBreak(s: String) {
        if (outgoingR63A != null) {
            byteBufOutputStream.writeBytes(s)
        }
    }

    fun writeShort(i: Int) = byteBufOutputStream.writeShort(i)

    fun writeInt(i: Int) {
        if (outgoingR63A != null) {
            HabboVl64.encodeBytes(i)?.let {
                byteBufOutputStream.write(it)
            }
        } else {
            byteBufOutputStream.writeInt(i)
        }
    }

    fun writeDouble(d: Double) = byteBufOutputStream.writeDouble(d)

    fun writeFloat(d: Float) = byteBufOutputStream.writeFloat(d)

    fun writeBoolean(b: Boolean) {
        if (outgoingR63A != null) {
            byteBufOutputStream.writeByte((if (b) 73 else 72))
        } else {
            byteBufOutputStream.writeBoolean(b)
        }
    }

    fun writeByte(b: Int) {
        byteBufOutputStream.writeByte(b)
    }

    fun serialize(habboResponseSerialize: IHabboResponseSerialize, vararg params: Any = arrayOf()) {
        if (outgoingR63A != null) {
            habboResponseSerialize.serializeHabboResponseR63A(this, *params)
        } else {
            habboResponseSerialize.serializeHabboResponse(this, *params)
        }
    }

    override fun toString(): String {
        var message = _byteBuf.toString(Charsets.UTF_8).replace("[\\r\\n]+".toRegex(), "(newline)")

        for (i in 0..31) message = message.replace(i.toChar().toString(), "[$i]")

        return message
    }

    override fun close() {
        byteBufOutputStream.close()

        _byteBuf.readerIndex(_byteBuf.readableBytes())
        _byteBuf.discardSomeReadBytes()

        ReferenceCountUtil.release(_byteBuf)
    }
}